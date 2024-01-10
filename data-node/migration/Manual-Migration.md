# Manual Migration Guide

**Caveat**: preliminary - and only to have a minimal basis to find out which modifications are necessary at the current codebase
and to research/test possible improvements for a better user experience or find out the limits what we can actually do.
Some of these steps could be useful in the future for PSO or support to migrate manually or fix problems that occurred during migration.

**Also: if you try to use this guide for a manual migration of a real prod system, all the preliniaries regarding certificates etc. probably 
don't match your use case. Please adjust accordingly!**

#### Testing during development

Create/include a locally generated Docker image for Graylog or the DataNode by using the `create-docker-images.sh`

At the time of writing this, if you test/develop on macOS and you're struggling with setting `vm.max_map_count` for Docker,
you're running the latest Docker version, which is incompatible with macOS Sonoma. Your only chance is to downgrade Docker.

## Things to watch out for
The following is a list of items that I did not put in much effort/time to test but should be kept in mind to avoid potential issues:
- keep hostnames as they are
- keep the cluster name

Otherwise you might create issues with existing meta data in OpenSearch.


In a lot of cases, missing a step or having an error, you have no other chance as to start over completely. Which might not be
possible in PROD environments. So make sure you have backups ;-)

## Migrating an existing OpenSearch 2.x or 1.3.x cluster

This is a preliminary guide to migrate an existing OpenSearch 2.x or 1.3.x cluster into a DataNode cluster.
Basis was the `cluster` from our `docker compose` github repository. But we only use one MongoDB and one Graylog node.

It is based on Docker to be able to reproduce all the steps consistently. Real installations will surely
differ. The steps can be also used to migrate a OS packages install.

## Migrating Elasticsearch 7.10

see additional info at the end of this document

### Contents of this directory:

- `docker-compose.yml` - the Docker compose file we're using and modifying in between steps
- `env` - the Environment variables read by `docker-compose.yml` for hashes and the admin password
- `cert.sh` - a script to create certificates for the OpenSearch cluster
- `custom-opensearch.yml` - certificate related OpenSearch configuration
- `opensearch-security-config.yml` - the OpenSearch security config that we`re going to replace during a migration
- `datanode-security-config.yml` - the default DataNode security config that we'll use as a replacement

### Migration Steps

You should be able to always start over by doing a `docker compose down -v` and taking back any modifications on the files.

#### The file `cert.sh`
Use the `cert.sh` script to generate the certificate certificates for the OpenSearch cluster. Use "password" as the password 
for certificate related stuff so that it matches the other scripts/files.

For a cluster that uses no certificates, don't create certificates. Remove the following lines
from each OpenSearch service in `docker-compose.yml`:

```
- "./root-ca.pem:/usr/share/opensearch/config/root-ca.pem"
- "./node?.pem:/usr/share/opensearch/config/node.pem"
- "./node?-key.pem:/usr/share/opensearch/config/node-key.pem"
- "./admin.pem:/usr/share/opensearch/config/admin.pem"
- "./admin-key.pem:/usr/share/opensearch/config/admin-key.pem"
- "./keystore.jks:/usr/share/opensearch/config/keystore.jks"
- "./custom-opensearch.yml:/usr/share/opensearch/config/opensearch.yml"
```
Replace the `?` with 1-3 depending on the service.

And remove the cert config from the graylog service by deleting the following lines:

```
      GRAYLOG_CA_KEYSTORE_FILE: "/usr/share/graylog/data/keystore.jks"
      GRAYLOG_CA_PASSWORD: "password"
```
and
```
      - "./keystore.jks:/usr/share/graylog/data/keystore.jks"
```

#### The file `env`
Modify the `env` file like with the regular docker compose examples that we provide. Rename it to `.env` .



#### The file `datanode-security-config.yml`
Convert the `GRAYLOG_PASSWORD_SECRET` to base64 e.g. by doing `echo "The password secret you chose" | base64` and
put it into line 131 of `datanode-security-config.yml`

### OpenSearch 1.3.x

To run this with OpenSearch 1.3.x, replace the used Docker image `2.10.0` with `1.3.1` for all 3 services.

### Create your containers

`docker compose create`

### Start Graylog with OpenSearch

Start your cluster `docker compose up -d mongodb opensearch1 opensearch2 opensearch3 graylog1`. Create an input, 
ingest some data. etc. 

Stop OpenSearch and Graylog with `docker compose stop graylog1 opensearch1 opensearch2 opensearch3`.

Modify all three OpenSearch services in `docker-compose.yml` to look like this:
```
       - "./opensearch-security-config.yml:/usr/share/opensearch/config/opensearch-security/config.yml"
#      - "./datanode-security-config.yml:/usr/share/opensearch/config/opensearch-security/config.yml"
```

So basically, switch out the default security config that uses a user/role based model to one that uses JWT authentication
which is the mechanism we use with the DataNode.

Bring the OpenSearch cluster back on: 

`docker compose create opensearch1 opensearch2 opensearch3`

`docker compose up -d opensearch1 opensearch2 opensearch3`

You can check the logs or test via `curl` that OpenSearch is back.

Use `docker ps` to get the ID of one of the nodes. `docker exec -it <ID> bash` into the container.
Run `cd /usr/share/opensearch/plugins/opensearch-security/tools` and `./securityadmin.sh -f /usr/share/opensearch/config/opensearch-security/config.yml -icl -h opensearch1 -nhnv -cacert ../../../config/root-ca.pem -cert ../../../config/admin.pem -key ../../../config/admin-key.pem` 
to make OpenSearch reload the security data.

This step is necessary so that the OpenSearch data directory with the security indices etc. contain the JWT auth data before we attach the data ddirectory to the DataNode.

Stop OpenSearch using `docker compose stop opensearch1 opensearch2 opensearch3`

Modify your graylog service so it will connect to DataNodes and show the Preflight config, it should look like this:

```
#      GRAYLOG_CA_KEYSTORE_FILE: "/usr/share/graylog/data/keystore.jks"
#      GRAYLOG_CA_PASSWORD: "password"
#      GRAYLOG_ELASTICSEARCH_HOSTS: "https://admin:admin@opensearch1:9200,https://admin:admin@opensearch2:9200,https://admin:admin@opensearch3:9200"
      GRAYLOG_ENABLE_PREFLIGHT_WEB: "true"
```

Issue `docker compose create graylog1` and `docker compose up -d graylog1 datanode1 datanode2 datanode3`.
Log into the Preflight UI at `http://localhost:9000` with the auth credentials from the end of the logs at `docker compose logs -f graylog1`

Provision certificates for the DataNodes, they should start up and get green. Resume the Graylog startup.
For now, nothing should happen - this will be addressed shortly in dev. Stop graylog with `docker compose stop graylog1`.

Disable the Preflight UI in `docker-compose.yml` by adding a `#`:
``` 
#      GRAYLOG_ENABLE_PREFLIGHT_WEB: "true"
```
Use `docker compose create graylog1` and `docker compose up -d graylog1` to bring graylog up again.

Now you should be able the log into graylog at `http://localhost:9000` with your regular credentials and have a running
configuration with DataNodes.


## Migrating Elasticsearch 7.10

Migration from Elasticsearch 7.10 needs an additional step. ES 7.10 does not understand JWT authentication.
So you have to first migrate to OpenSearch before running the update of the security information.
Look at the supplied `es710-docker-compose.yml` as an example. Please note that except for the servicename, I changed the cluster name
and hostnames etc. to `opensearch`. In a regular setting, it would be the other way around and you would have to pull the
elasticsearch names through the whole process into the DataNode.

Start the Elasticsearch cluster, add some data etc. `docker compose up -d elasticsearch1 elasticsearch2 elasticsearch3`
stop it again `docker compose stop elasticsearch1 elasticsearch2 elasticsearch3`.

Start the OpenSearch cluster in place of the Elasticsearch cluster. It points to the same data directory.
`docker compose up -d opensearch1 opensearch2 opensearch3`. The `es710-docker-compose.yml` already points to the
security config with the JWT auth settings. Make sure you added the correct bas64 encoded secret.

Run the `securityadmin.sh` as described above and just follow the steps for an OpenSearch 1.3 migration as described.

Please also note that the ElasticSearch example does not contain any certificates for ElasticSearch but uses generated certificates 
once you started the OpenSearch cluster.
