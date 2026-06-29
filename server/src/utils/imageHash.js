const sharp = require('sharp');

async function getHash(imagePath) {
  // Resize to 8x8 greyscale, then compute average hash
  const { data } = await sharp(imagePath)
    .resize(8, 8, { fit: 'fill' })
    .greyscale()
    .raw()
    .toBuffer({ resolveWithObject: true });

  const avg = data.reduce((sum, val) => sum + val, 0) / data.length;
  return Array.from(data).map(val => (val >= avg ? '1' : '0')).join('');
}

function hammingDistance(hash1, hash2) {
  let distance = 0;
  for (let i = 0; i < Math.min(hash1.length, hash2.length); i++) {
    if (hash1[i] !== hash2[i]) distance++;
  }
  return distance;
}

function similarityScore(hash1, hash2) {
  const maxDist = hash1.length;
  const dist = hammingDistance(hash1, hash2);
  return 1 - dist / maxDist;
}

module.exports = { getHash, hammingDistance, similarityScore };