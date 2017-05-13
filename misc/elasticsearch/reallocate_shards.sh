#!/bin/bash -x

if [ $# -lt 1 ];then
        echo $0 index_name chunk_id
        echo "example $0 graylog2_41 0"
        exit -1
fi

# SRVS is all your elasticsearch nodes
SRVS="gles01 gles02 gles03"
index_name=$1
chunk_id=$2
for  i in $SRVS;do
	echo "$i"
curl -X POST     'http://'${i}':9200/_cluster/reroute?pretty=true'     -d '{ "commands" : [ { "allocate" : { "index" : "'${index_name}'", "shard" : '${chunk_id}' , "node" : "'${i}'", "allow_primary" : 1 }}]}'
done

