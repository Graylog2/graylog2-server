#!/bin/bash -e
ACTIVATOR_VERISON='1.2.10'
ACTIVATOR_URL="http://downloads.typesafe.com/typesafe-activator/${ACTIVATOR_VERISON}/typesafe-activator-${ACTIVATOR_VERISON}-minimal.zip"

ACTIVATOR_BIN=$(which activator &>/dev/null || true)

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

read -p "Did you bump both app/lib/Version.java and project/Build.scala? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
  echo "Great! Starting the build process."
else
  echo "Please do that."
  exit 1
fi  

# Clean working directory
"${ACTIVATOR_BIN}" clean

# Prepare JavaScript
pushd javascript
npm test
node_modules/.bin/gulp deploy-prod
popd

# Build universal .tar.gz
"${ACTIVATOR_BIN}" universal:package-zip-tarball

date
echo "Your package(s) are ready in 'target/universal':"
echo
ls -lt ./target/universal
