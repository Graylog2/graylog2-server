#!/bin/sh 
if [ $# -lt 1 ];then
	echo $0 index_name_to_make_writable
	exit -1
fi
# any node in elasticsearch cluster
HOST=gles01
INDEXNAME=$1
ACTION=${2-false}
export HOST INDEXNAME
set -x
curl -XPOST http://${HOST}:9200/${INDEXNAME}/_close 2> /dev/null |json_pp
curl -XPUT http://${HOST}:9200/${INDEXNAME}/_settings -d '
{
    "index" : {
        "blocks" : {
            "write" : "'$ACTION'"
         }
    }
}' 2> /dev/null |json_pp

curl -XPOST http://${HOST}:9200/${INDEXNAME}/_open 2>/dev/null |json_pp
