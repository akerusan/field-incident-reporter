/**
 * Local mock API for Field Incident Reporter.
 *
 * json-server provides GET, PATCH and DELETE on /incidents from db.json. On top of it, this file
 * adds the parts of the contract (openapi.yaml) that json-server doesn't do by itself:
 *  - PUT /incidents/:id is an upsert, so the app can create and update with the same idempotent
 *    request, using client-generated ids.
 *  - PUT and DELETE are rejected with 409 once an operator has resolved the incident.
 *  - POST /uploads stores a photo and returns its relative path.
 *  - Request validation and a single error body shape: { code, message }.
 *
 * Environment variables:
 *  PORT         port to listen on (default 3000)
 *  DB_FILE      database file (default ./db.json)
 *  DELAY_MS     delay every request, to demo loading states (default 0)
 *  FAIL_STATUS  answer every app write (PUT/DELETE on /incidents) with this status, to demo
 *               retry and failure handling (default: off)
 */
const crypto = require('node:crypto');
const path = require('node:path');
const jsonServer = require('json-server');
const multer = require('multer');

const PORT = Number(process.env.PORT ?? 3000);
const DB_FILE = process.env.DB_FILE ?? path.join(__dirname, 'db.json');
const DELAY_MS = Number(process.env.DELAY_MS ?? 0);
const FAIL_STATUS = process.env.FAIL_STATUS ? Number(process.env.FAIL_STATUS) : null;
const PUBLIC_DIR = path.join(__dirname, 'public');
const UPLOADS_DIR = path.join(PUBLIC_DIR, 'uploads');
const MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

const CATEGORIES = ['EQUIPMENT', 'ROAD_HAZARD', 'ELECTRICAL', 'PLUMBING', 'SAFETY', 'OTHER'];
const SEVERITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED'];
const IMAGE_EXTENSIONS = { 'image/jpeg': '.jpg', 'image/png': '.png', 'image/webp': '.webp' };

const server = jsonServer.create();
const router = jsonServer.router(DB_FILE);
const incidents = () => router.db.get('incidents');

function sendError(res, status, code, message) {
  res.status(status).json({ code, message });
}

// --- Validation ---------------------------------------------------------------------------------

const isNonEmptyString = (value) => typeof value === 'string' && value.trim().length > 0;
// A UTC instant such as 2026-11-07T08:30:00Z or 2026-11-07T08:30:00.123Z. Strict on purpose: clients
// parse it as an ISO-8601 instant, and a looser format would make them skip the incident.
const ISO_INSTANT = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,9})?Z$/;
const isIsoInstant = (value) => typeof value === 'string' && ISO_INSTANT.test(value) && !Number.isNaN(Date.parse(value));

/** Returns a list of problems with a full incident body (PUT), empty when valid. */
function validateIncident(body) {
  const problems = [];
  if (!isNonEmptyString(body.title)) problems.push('title is required');
  if (typeof body.description !== 'string') problems.push('description must be a string');
  if (!CATEGORIES.includes(body.category)) problems.push(`category must be one of ${CATEGORIES.join(', ')}`);
  if (!SEVERITIES.includes(body.severity)) problems.push(`severity must be one of ${SEVERITIES.join(', ')}`);
  if (!STATUSES.includes(body.status)) problems.push(`status must be one of ${STATUSES.join(', ')}`);
  if (typeof body.locationNote !== 'string') problems.push('locationNote must be a string');
  if (!isIsoInstant(body.reportedAt)) problems.push('reportedAt must be an ISO-8601 UTC instant (…Z)');
  if (!isIsoInstant(body.updatedAt)) problems.push('updatedAt must be an ISO-8601 UTC instant (…Z)');
  if (!Array.isArray(body.photos)) {
    problems.push('photos must be an array');
  } else if (!body.photos.every((photo) => isNonEmptyString(photo?.id) && isNonEmptyString(photo?.path))) {
    problems.push('each photo needs an id and a path');
  }
  return problems;
}

/** Only the fields an operator may change through PATCH (triage). */
function validatePatch(body) {
  const allowed = ['status', 'updatedAt'];
  const problems = Object.keys(body)
    .filter((key) => !allowed.includes(key))
    .map((key) => `${key} cannot be patched`);
  if (body.status !== undefined && !STATUSES.includes(body.status)) {
    problems.push(`status must be one of ${STATUSES.join(', ')}`);
  }
  if (body.updatedAt !== undefined && !isIsoInstant(body.updatedAt)) {
    problems.push('updatedAt must be an ISO-8601 UTC instant (…Z)');
  }
  return problems;
}

/** Keeps only the fields defined by the contract, so clients can't store arbitrary data. */
function toStoredIncident(id, body) {
  return {
    id,
    title: body.title.trim(),
    description: body.description,
    category: body.category,
    severity: body.severity,
    status: body.status,
    locationNote: body.locationNote,
    photos: body.photos.map((photo) => ({ id: photo.id, path: photo.path })),
    reportedAt: body.reportedAt,
    updatedAt: body.updatedAt,
  };
}

// --- Middleware ---------------------------------------------------------------------------------

server.use(jsonServer.defaults({ static: PUBLIC_DIR }));
server.use(jsonServer.bodyParser);

if (DELAY_MS > 0) {
  server.use((req, res, next) => setTimeout(next, DELAY_MS));
}

if (FAIL_STATUS) {
  server.use('/incidents/:id', (req, res, next) => {
    if (req.method === 'PUT' || req.method === 'DELETE') {
      sendError(res, FAIL_STATUS, 'SIMULATED_FAILURE', `Simulated failure (FAIL_STATUS=${FAIL_STATUS})`);
      return;
    }
    next();
  });
}

// An operator's resolution is final for the app: technicians can't overwrite or delete it.
// Without this rule, a technician's queued edit would silently un-resolve the incident.
server.use('/incidents/:id', (req, res, next) => {
  if (req.method !== 'PUT' && req.method !== 'DELETE') return next();
  const stored = incidents().find({ id: req.params.id }).value();
  if (stored?.status === 'RESOLVED') {
    sendError(res, 409, 'INCIDENT_RESOLVED', 'This incident was resolved by an operator and can no longer be changed.');
    return;
  }
  next();
});

// --- Routes -------------------------------------------------------------------------------------

// Creation goes through PUT with a client-generated id; see openapi.yaml.
server.post('/incidents', (req, res) => {
  sendError(res, 405, 'METHOD_NOT_ALLOWED', 'Create incidents with PUT /incidents/{id}.');
});

server.put('/incidents/:id', (req, res) => {
  const problems = validateIncident(req.body);
  if (problems.length > 0) {
    sendError(res, 400, 'VALIDATION_ERROR', problems.join('; '));
    return;
  }
  const incident = toStoredIncident(req.params.id, req.body);
  const list = incidents().value();
  const index = list.findIndex((item) => item.id === incident.id);
  if (index === -1) {
    list.push(incident);
  } else {
    list[index] = incident;
  }
  router.db.write();
  res.status(index === -1 ? 201 : 200).json(incident);
});

server.patch('/incidents/:id', (req, res, next) => {
  const problems = validatePatch(req.body);
  if (problems.length > 0) {
    sendError(res, 400, 'VALIDATION_ERROR', problems.join('; '));
    return;
  }
  next(); // json-server applies the patch
});

const upload = multer({
  storage: multer.diskStorage({
    destination: UPLOADS_DIR,
    filename: (req, file, callback) => callback(null, `${crypto.randomUUID()}${IMAGE_EXTENSIONS[file.mimetype]}`),
  }),
  limits: { fileSize: MAX_UPLOAD_BYTES, files: 1 },
  fileFilter: (req, file, callback) => callback(null, file.mimetype in IMAGE_EXTENSIONS),
});

server.post('/uploads', (req, res) => {
  upload.single('file')(req, res, (error) => {
    if (error) {
      const tooLarge = error.code === 'LIMIT_FILE_SIZE';
      sendError(res, tooLarge ? 413 : 400, tooLarge ? 'FILE_TOO_LARGE' : 'INVALID_UPLOAD', error.message);
      return;
    }
    if (!req.file) {
      sendError(res, 400, 'INVALID_UPLOAD', 'Send one JPEG, PNG or WebP image in the "file" field.');
      return;
    }
    res.status(201).json({ path: `/uploads/${req.file.filename}` });
  });
});

server.use(router);

server.listen(PORT, () => {
  console.log(`Mock API on http://localhost:${PORT} (db: ${path.relative(process.cwd(), DB_FILE) || DB_FILE})`);
  if (DELAY_MS > 0) console.log(`  DELAY_MS=${DELAY_MS}`);
  if (FAIL_STATUS) console.log(`  FAIL_STATUS=${FAIL_STATUS}: app writes will fail`);
});
