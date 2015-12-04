#!/bin/bash -e

# Install same npm version as we use in travis
rm -rf ./node_modules
npm install --no-spin npm@latest-2

echo -n "Using npm "
./node_modules/.bin/npm --version

./node_modules/.bin/npm install --no-spin
./node_modules/.bin/npm test
./node_modules/.bin/npm run build

echo
echo '# BUILD COMPLETE'

