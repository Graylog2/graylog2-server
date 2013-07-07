#!/bin/bash

MONGODB_PASSWORD=""
RABBITMQ_PASSWORD=""

if [ "$MONGODB_PASSWORD" == "" ] || [ "$RABBITMQ_PASSWORD" == "" ]; then
    echo "Please specify appropriate passwords in the script before running running it"
    exit 1
fi

# prep the instance
sudo apt-get update
sudo apt-get -y install zlib1g-dev
sudo apt-get -y install libssl-dev libreadline-gplv2-dev
sudo apt-get -y install ruby1.9.3
sudo apt-get -y install rubygems
sudo apt-get -y install openjdk-7-jre-headless
update-alternatives --set ruby /usr/bin/ruby1.9.3

# mongodb
sudo apt-key adv --keyserver keyserver.ubuntu.com --recv 7F0CEB10
echo 'deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen' | sudo tee /etc/apt/sources.list.d/10gen.list
sudo apt-get -y update
sudo apt-get -y install mongodb-10gen

# elastic search
curl -LO https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-0.20.6.deb
sudo dpkg -i elasticsearch-0.20.6.deb
sudo service elasticsearch stop
sudo sed -i 's/# cluster.name: elasticsearch/cluster.name: graylog2/' /etc/elasticsearch/elasticsearch.yml
sudo service elasticsearch start

# graylog2
sudo apt-get -y install openjdk-7-jre
sudo mkdir -p /opt/graylog2

curl -OL http://download.graylog2.org/graylog2-server/graylog2-server-0.11.0.tar.gz
tar -xvf graylog2-server-0.11.0.tar.gz
sudo cp -r graylog2-server-0.11.0 /opt/graylog2

# configure graylog2-server
pushd /opt/graylog2
    sudo ln -sf graylog2-server-0.11.0 graylog2-server
    sudo cp graylog2-server/graylog2.conf.example /etc/graylog2.conf
    sudo cp graylog2-server/elasticsearch.yml.example /etc/graylog2-elasticsearch.yml
    sudo sed -i -e "s/mongodb_password = 123/mongodb_password = $MONGODB_PASSWORD/" \
        -e "s/amqp_enabled = false/amqp_enabled = true/" \
        -e "s/amqp_host = localhost/amqp_host = rabbit.trustpilot.com/" \
        -e "s/amqp_username = guest/amqp_username = admin/" \
        -e "s/amqp_password = guest/amqp_password = $RABBITMQ_PASSWORD/" /etc/graylog2.conf
popd

# add a user to mongodb
cat <<EOF | mongo
use graylog2
db.addUser("grayloguser", "$MONGODB_PASSWORD")
exit
EOF

# graylog2 server init script
sudo mv graylog2-server-init-script /etc/init.d/graylog2-server
sudo chmod +x /etc/init.d/graylog2-server

# start graylog2 server
sudo service graylog2-server start

# graylog2 web interface
curl -OL http://download.graylog2.org/graylog2-web-interface/graylog2-web-interface-0.11.0.tar.gz
tar -xvf graylog2-web-interface-0.11.0.tar.gz
sudo cp -r graylog2-web-interface-0.11.0 /opt/graylog2
pushd /opt/graylog2
    sudo ln -sf graylog2-web-interface-0.11.0 graylog2-web-interface
    pushd graylog2-web-interface
	sudo gem install bundler
    sudo sed -i "s/source :rubygems/source 'https:\/\/rubygems.org'/" Gemfile
	sudo bundle install
    popd
popd
sudo useradd -m graylog
sudo chown -R graylog:graylog /opt/graylog2/graylog2-web-*
sudo apt-get -y install gcc libpcre3-dev zlib-bin zlib1g zlib1g-dbg zlib1g-dev openssl libssl-dev gobjc++ libcurl4-openssl-dev apache2
sudo gem install passenger
sudo gem install file-tail

# graylog2 web interface init script
sudo mv graylog2-web-interface-init-script /etc/init.d/graylog2-web-interface
sudo chmod +x /etc/init.d/graylog2-web-interface

# start graylog2 web interface
sudo service graylog2-web-interface start
sudo service graylog2-web-interface stop
sudo chown -R graylog:graylog /opt/graylog2/graylog2-web-*
sudo service graylog2-web-interface start
