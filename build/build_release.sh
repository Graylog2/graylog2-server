#!/bin/bash

BUILD_NUMBER=$1
BUILD_NAME=graylog2-web-interface-$BUILD_NUMBER
BUILD_DIR=builds/$BUILD_NAME
BUILD_DATE=`date`
LOGFILE=`pwd`/logs/$BUILD_NAME

# Check if required version parameter is given
if [ -z $BUILD_NUMBER ]; then
  echo "ERROR: Missing parameter. (build number)"
  exit 1
fi

# Create directories
mkdir -p logs
mkdir -p builds
mkdir -p $BUILD_DIR

# Create logfile
touch $LOGFILE
date >> $LOGFILE

echo "BUILDING $BUILD_NAME"

# Add build date to release.
echo $BUILD_DATE > $BUILD_DIR/build_date

echo "Copying files ..."

# Copy files.
cp -R ../app ../config ../lib ../public ../Rakefile ../README ../Gemfile ../Gemfile.lock ../config.ru ../script ../vendor $BUILD_DIR

# Remove not needed files
rm $BUILD_DIR/public/images/dashboard_logo.png

echo "Configuring release ..."

# We are not in the build directory
cd $BUILD_DIR

# Change config files.
mv config/general.yml.example config/general.yml
mv config/mongoid.yml.example config/mongoid.yml
mv config/indexer.yml.example config/indexer.yml
mv config/ldap.yml.example config/ldap.yml

RAILS_ENV=production
export RAILS_ENV

# Set Standard rails environment
echo "RAILS_ENV = 'production'" > temp_env
cat config/environment.rb >> temp_env
mv temp_env config/environment.rb

# tar it
cd ..
echo "Building Tarball ..."
gnutar cfz $BUILD_NAME.tar.gz $BUILD_NAME
rm -rf ./$BUILD_NAME

echo "DONE! Created Graylog2 release $BUILD_NAME on $BUILD_DATE"
