/**
 * Prepares the runtime state from the committed seed:
 *  - db.seed.json      -> db.json
 *  - seed/uploads/*    -> public/uploads/
 *
 * `node scripts/seed.js` only fills in what's missing, so `npm start` keeps your data.
 * `node scripts/seed.js --force` (`npm run reset`) restores the seed and removes uploaded photos.
 */
const fs = require('node:fs');
const path = require('node:path');

const root = path.join(__dirname, '..');
const force = process.argv.includes('--force');
const dbFile = path.join(root, 'db.json');
const seedDbFile = path.join(root, 'db.seed.json');
const uploadsDir = path.join(root, 'public', 'uploads');
const seedUploadsDir = path.join(root, 'seed', 'uploads');

fs.mkdirSync(uploadsDir, { recursive: true });

if (force) {
  for (const name of fs.readdirSync(uploadsDir)) {
    if (name !== '.gitkeep') fs.rmSync(path.join(uploadsDir, name));
  }
}

if (force || !fs.existsSync(dbFile)) {
  fs.copyFileSync(seedDbFile, dbFile);
  console.log('db.json created from db.seed.json');
}

for (const name of fs.readdirSync(seedUploadsDir)) {
  const target = path.join(uploadsDir, name);
  if (!fs.existsSync(target)) fs.copyFileSync(path.join(seedUploadsDir, name), target);
}
