#!/bin/bash -e
SBT_VERISON='0.13.8'
SBT_URL="https://dl.bintray.com/sbt/native-packages/sbt/${SBT_VERSION}/sbt-${SBT_VERSION}.tgz"

SBT_BIN=$(which sbt)

if [[ -z "${SBT_BIN}" ]]; then
  SBT_BIN="${SBT_PATH}/bin/sbt"

  if [[ ! -x "${SBT_BIN}" ]]; then
    echo "ERROR: Couldn't find SBT in \$PATH or \$SBT_PATH."
    echo
    echo "Please download and install SBT before running this script:"
    echo
    echo "  $ wget ${SBT_URL}"
    echo "  $ brew install sbt ## Homebrew (Third-party package)"
    echo "  $ port install sbt ## Macports (Third-party package)"
    echo
    exit 1
  fi
fi

# Clean working directory
"${SBT_BIN}" clean

########################
## Prepare JavaScript ##
########################
pushd javascript

# Install same npm version as we use in travis
rm -rf ./node_modules
npm install --no-spin npm@latest

echo -n "Using npm "
./node_modules/.bin/npm --version

./node_modules/.bin/npm install --no-spin
./node_modules/.bin/npm test
./node_modules/.bin/npm run build
popd


############################
## Build Play application ##
############################
"${SBT_BIN}" update
"${SBT_BIN}" compile
"${SBT_BIN}" test

# Build universal .tar.gz
"${SBT_BIN}" universal:package-zip-tarball

date
echo "Your package(s) are ready in 'target/universal':"
echo
ls -lt ./target/universal/graylog-web-interface-*.tgz

echo
echo '# Calculating artifact checksums'
pushd ./target/universal
for ARTIFACT in graylog-web-interface-*.tgz
do
  echo
  echo -n "MD5:    "
  md5sum "${ARTIFACT}" | tee "${ARTIFACT}.md5.txt"
  echo -n "SHA1:   "
  sha1sum "${ARTIFACT}" | tee "${ARTIFACT}.sha1.txt"
  echo -n "SHA256: "
  sha256sum "${ARTIFACT}" | tee "${ARTIFACT}.sha256.txt"
done
popd

echo
echo '# BUILD COMPLETE'
