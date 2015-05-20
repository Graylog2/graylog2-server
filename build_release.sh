#!/bin/bash -e
ACTIVATOR_VERISON='1.2.10'
ACTIVATOR_URL="http://downloads.typesafe.com/typesafe-activator/${ACTIVATOR_VERISON}/typesafe-activator-${ACTIVATOR_VERISON}-minimal.zip"

ACTIVATOR_BIN=$(which activator)

if [[ -z "${ACTIVATOR_BIN}" ]]; then
  ACTIVATOR_BIN="${ACTIVATOR_PATH}/activator"

  if [[ ! -x "${ACTIVATOR_BIN}" ]]; then
    echo "ERROR: Couldn't find Typesafe Activator in \$PATH or \$ACTIVATOR_PATH."
    echo
    echo "Please download and install Typesafe Activator before running this script:"
    echo
    echo "  wget ${ACTIVATOR_URL}"
    echo
    exit 1
  fi
fi

# Clean working directory
"${ACTIVATOR_BIN}" clean

# Prepare JavaScript
pushd javascript

# Install same npm version as we use in travis
rm -rf ./node_modules
npm install npm@latest
PATH="$(pwd)/node_modules/.bin/":$PATH

npm install
npm test
gulp deploy-prod
popd

# Build universal .tar.gz
"${ACTIVATOR_BIN}" universal:package-zip-tarball

date
echo "Your package(s) are ready in 'target/universal':"
echo
ls -lt ./target/universal/graylog-web-interface-*.tgz

echo
echo "* MD5 hash"
md5sum ./target/universal/graylog-web-interface-*.tgz
echo "* SHA256 hash"
sha256sum ./target/universal/graylog-web-interface-*.tgz
