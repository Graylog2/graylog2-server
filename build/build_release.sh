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
cp ../app ../config ../db ../lib ../public ../Rakefile ../README ../script ../test ../vendor $BUILD_DIR -r

# Copy example database.yml file
cp database.yml.example $BUILD_DIR/config/database.yml
cp graphs.yml.example $BUILD_DIR/config/graphs.yml
cp mongodb.yml.example $BUILD_DIR/config/mongodb.yml

# Remove not needed files
rm $BUILD_DIR/public/images/dashboard_logo.png

echo "Configuring release ..."

# We are not in the build directory
cd $BUILD_DIR
RAILS_ENV=production
export RAILS_ENV

# Set Standard rails environment
echo "RAILS_ENV = 'production'" > temp_env
cat config/environment.rb >> temp_env
mv temp_env config/environment.rb

# Remove newrelic_rpm plugin which is not needed for production
script/plugin remove newrelic_rpm >> $LOGFILE 2>&1
rm config/newrelic.yml

echo "Freezing Rails and gems ..."

# Freeze Rails
rake rails:freeze:gems >> $LOGFILE 2>&1

# Freeze the gems
rake gems:unpack >> $LOGFILE 2>&1

# Remove gem requirements that cause errors later. Only needed as freeze definition. (TODO: There must be a better way to do that)
sed '/bson_ext/d' config/environment.rb > environment.rb_tmp && mv environment.rb_tmp config/environment.rb
sed '/nunemaker-validatable/d' config/environment.rb > environment.rb_tmp && mv environment.rb_tmp config/environment.rb

# tar it
cd ..
echo "Building Tarball ..."
tar cfz $BUILD_NAME.tar.gz $BUILD_NAME
rm -rf ./$BUILD_NAME

echo "DONE! Created Graylog2 release $BUILD_NAME on $BUILD_DATE"
