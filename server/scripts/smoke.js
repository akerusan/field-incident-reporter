/**
 * End-to-end check of the API contract. Starts the server on a temporary copy of the seed
 * database, walks through the requests the app and the dashboard make, then cleans up.
 *
 *   npm run smoke
 */
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const crypto = require('node:crypto');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const root = path.join(__dirname, '..');
const port = 3999;
const baseUrl = `http://localhost:${port}`;
const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'fir-smoke-'));
const dbFile = path.join(tempDir, 'db.json');
fs.copyFileSync(path.join(root, 'db.seed.json'), dbFile);

const serverProcess = spawn(process.execPath, [path.join(root, 'index.js')], {
  env: { ...process.env, PORT: String(port), DB_FILE: dbFile, DELAY_MS: '0', FAIL_STATUS: '' },
  stdio: ['ignore', 'pipe', 'inherit'],
});

async function waitForServer() {
  for (let attempt = 0; attempt < 50; attempt++) {
    try {
      const response = await fetch(`${baseUrl}/incidents`);
      if (response.ok) return;
    } catch {
      // not listening yet
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error('server did not start');
}

async function request(method, urlPath, body) {
  const response = await fetch(`${baseUrl}${urlPath}`, {
    method,
    headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  return { status: response.status, body: text ? JSON.parse(text) : null };
}

function step(name) {
  console.log(`  ✓ ${name}`);
}

async function run() {
  await waitForServer();
  const id = crypto.randomUUID();
  const now = new Date().toISOString();
  const incident = {
    id,
    title: 'Smoke test incident',
    description: 'Created by scripts/smoke.js',
    category: 'EQUIPMENT',
    severity: 'LOW',
    status: 'OPEN',
    locationNote: 'Test bench',
    photos: [],
    reportedAt: now,
    updatedAt: now,
  };

  let response = await request('GET', '/incidents?_sort=reportedAt&_order=desc');
  assert.equal(response.status, 200);
  assert.ok(response.body.length >= 8);
  const reportedTimes = response.body.map((item) => Date.parse(item.reportedAt));
  assert.deepEqual(reportedTimes, [...reportedTimes].sort((a, b) => b - a));
  step('GET /incidents lists the seed, newest first');

  response = await request('GET', '/incidents?status=RESOLVED');
  assert.ok(response.body.length > 0 && response.body.every((item) => item.status === 'RESOLVED'));
  step('GET /incidents?status= filters');

  response = await request('PUT', `/incidents/${id}`, incident);
  assert.equal(response.status, 201);
  step('PUT on a new id creates (201)');

  response = await request('PUT', `/incidents/${id}`, incident);
  assert.equal(response.status, 200);
  response = await request('GET', `/incidents?id=${id}`);
  assert.equal(response.body.length, 1);
  step('repeating the PUT is idempotent (200, still one incident)');

  response = await request('PUT', `/incidents/${id}`, { ...incident, title: '' });
  assert.equal(response.status, 400);
  assert.equal(response.body.code, 'VALIDATION_ERROR');
  step('PUT with an invalid body is rejected (400 VALIDATION_ERROR)');

  for (const photos of [
    [{ id: crypto.randomUUID(), path: '//other-host/photo.jpg' }],
    [{ id: crypto.randomUUID(), path: 'http://other-host/photo.jpg' }],
    [{ id: crypto.randomUUID(), path: '/uploads/../db.json' }],
    [{ id: 'not-a-uuid', path: '/uploads/hvac-1.jpg' }],
  ]) {
    response = await request('PUT', `/incidents/${id}`, { ...incident, photos });
    assert.equal(response.status, 400, JSON.stringify(photos));
  }
  const photoId = crypto.randomUUID();
  const duplicate = { id: photoId, path: '/uploads/hvac-1.jpg' };
  response = await request('PUT', `/incidents/${id}`, { ...incident, photos: [duplicate, duplicate] });
  assert.equal(response.status, 400);
  step('photos need a UUID id, a unique id and a path under /uploads (400 otherwise)');

  response = await request('PUT', `/incidents/${id}`, { ...incident, status: 'RESOLVED' });
  assert.equal(response.status, 400);
  step('PUT cannot resolve an incident; only operators do, with PATCH (400)');

  response = await request('PATCH', `/incidents/${id}`, { status: 'IN_PROGRESS', updatedAt: '2026-11-07' });
  assert.equal(response.status, 400);
  step('timestamps must be ISO-8601 UTC instants (400 otherwise)');

  response = await request('POST', '/incidents', incident);
  assert.equal(response.status, 405);
  step('POST /incidents is not part of the contract (405)');

  const form = new FormData();
  const photo = fs.readFileSync(path.join(root, 'seed', 'uploads', 'hvac-1.jpg'));
  form.append('file', new Blob([photo], { type: 'image/jpeg' }), 'photo.jpg');
  const uploadResponse = await fetch(`${baseUrl}/uploads`, { method: 'POST', body: form });
  const upload = await uploadResponse.json();
  assert.equal(uploadResponse.status, 201);
  assert.match(upload.path, /^\/uploads\/[0-9a-f-]+\.jpg$/);
  const served = await fetch(`${baseUrl}${upload.path}`);
  assert.equal(served.status, 200);
  step('POST /uploads returns a relative path that is served');

  response = await request('PATCH', `/incidents/${id}`, { status: 'RESOLVED', updatedAt: new Date().toISOString() });
  assert.equal(response.status, 200);
  assert.equal(response.body.status, 'RESOLVED');
  step('PATCH triages the status');

  response = await request('PATCH', `/incidents/${id}`, { title: 'not allowed' });
  assert.equal(response.status, 400);
  step('PATCH rejects fields other than status/updatedAt (400)');

  response = await request('PUT', `/incidents/${id}`, incident);
  assert.equal(response.status, 409);
  assert.equal(response.body.code, 'INCIDENT_RESOLVED');
  response = await request('DELETE', `/incidents/${id}`);
  assert.equal(response.status, 409);
  step('PUT and DELETE on a RESOLVED incident are rejected (409 INCIDENT_RESOLVED)');

  response = await request('PATCH', `/incidents/${id}`, { status: 'OPEN' });
  assert.equal(response.status, 200);
  step('PATCH can reopen a resolved incident');

  response = await request('DELETE', `/incidents/${id}`);
  assert.equal(response.status, 200);
  response = await request('DELETE', `/incidents/${id}`);
  assert.equal(response.status, 404);
  step('DELETE removes the incident; a repeated DELETE is a 404 (the app treats it as done)');

  fs.rmSync(path.join(root, 'public', upload.path));
}

run()
  .then(() => {
    console.log('Smoke test passed.');
    process.exitCode = 0;
  })
  .catch((error) => {
    console.error('Smoke test failed:', error);
    process.exitCode = 1;
  })
  .finally(() => {
    serverProcess.kill();
    fs.rmSync(tempDir, { recursive: true, force: true });
  });
