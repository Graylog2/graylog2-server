#!/bin/bash
# script to build local docker images for graylog and the datanode
#
# put this script in the root dir where you have graylog checked out, also check out the graylog-docker repo in the same dir.
# you should have the following structure:
#
# ls [enter]
# create-docker-images.sh
# graylog-docker
# graylog-project-internal
# graylog-project-repos
# 
# run the script after a "mvn -DskipTests clean package" to build the images
#
# put the following lines in your .env file to reference locally created docker images
# DATANODE_IMAGE=graylog/graylog-datanode:local
# GRAYLOG_IMAGE=graylog/graylog:local
#

cd graylog-docker

cp `ls -1 ../graylog-project-internal/target/artifacts/graylog-datanode/graylog-datanode-6.0.0-SNAPSHOT-*-linux-x64.tar.gz|tail -1` .
docker build -t graylog/graylog-datanode:local --build-arg JAVA_VERSION_MAJOR=17 --build-arg LOCAL_BUILD_TGZ=`ls -1 graylog-datanode-6.0.0-SNAPSHOT-*-linux-x64.tar.gz|tail -1` -f docker/datanode/Dockerfile .
rm `ls -1 graylog-datanode-6.0.0-SNAPSHOT-*-linux-x64.tar.gz|tail -1`

cp `ls -1 ../graylog-project-internal/target/artifacts/graylog-enterprise/graylog-enterprise-6.0.0-SNAPSHOT-*-linux-x64.tar.gz|tail -1` .
docker build -t graylog/graylog:local --build-arg JAVA_VERSION_MAJOR=17 --build-arg LOCAL_BUILD_TGZ=`ls -1 graylog-enterprise-6.0.0-SNAPSHOT-*-linux-x64.tar.gz|tail -1` -f docker/enterprise/Dockerfile .
rm `ls -1 graylog-enterprise-6.0.0-SNAPSHOT-*-linux-x64.tar.gz|tail -1`

cd ..
