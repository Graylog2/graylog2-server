#!/bin/sh
if [ $# -lt 1 ];then
	echo $0 index_name
	exit -1
fi
# one of nodes in cluster
HOST=gles01
INDEXNAME=$1

curl -XDELETE http://${HOST}:9200/${INDEXNAME}/_query -d '
{
	"query":{
		"filtered":{
			"query":{
				"query_string":{
					"query":"somethingannoyingyouwouldliketoremove"
				}
			}
		}
	}
}
' 2> /dev/null |json_pp
	
#					"query":"repeated \\[23\\] times"
#					"query":"\\device\\harddiskvolume2\\program files (x86)\\nxlog\\nxlog.exe"
#					"query":"META-INF\\services\\org.xml.sax.driver"

