#!/bin/bash

# based on https://github.com/fabian/Elastica/blob/master/test/bin/run_elasticsearch.sh

wget https://github.com/downloads/elasticsearch/elasticsearch/elasticsearch-0.18.5.tar.gz
tar -xzf elasticsearch-0.18.5.tar.gz
sed 's/# index.number_of_shards: 1/index.number_of_shards: 1/' elasticsearch-0.18.5/config/elasticsearch.yml > elasticsearch-0.18.5/config/elasticsearch.yml
sed 's/# index.number_of_replicas: 0/index.number_of_replicas: 0/' elasticsearch-0.18.5/config/elasticsearch.yml > elasticsearch-0.18.5/config/elasticsearch.yml
sed 's/# discovery.zen.ping.multicast.enabled: false/discovery.zen.ping.multicast.enabled: false/' elasticsearch-0.18.5/config/elasticsearch.yml > elasticsearch-0.18.5/config/elasticsearch.yml

export JAVA_OPTS="-server"
elasticsearch-0.18.5/bin/elasticsearch &

echo "Waiting until elasticsearch is ready on port 9200"
while [[ -z `curl -s 'http://localhost:9200'` ]]
do
	echo -n "."
	sleep 2s
done

echo "elasticsearch is up"
