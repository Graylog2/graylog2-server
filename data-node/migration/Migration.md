# Migration Guide

Caveat: preliminary - and only to have a minimal basis to find out which modifications are necessary at the current codebase
and to research/test possible improvements for a better user experience or find out the limits what we can actually do.
Some of these steps could be useful in the future for PSO or support to migrate manually or fix problems that occurred during migration.

#### Testing during development

Create/include a locally generated Docker image for Graylog or the DataNode by using the `create-docker-images.sh`

At the time of writing this, if you test/develop on macOS and you're struggling with setting `vm.max_map_count` for Docker,
you're running the latest Docker version, which is incompatible with macOS Sonoma. Your only chance is to downgrade Docker.

## Migrating an existing OpenSearch 2.x cluster

This is a preliminary guide to migrate an existing OpenSearch 2.x cluster into a DataNode cluster.
Basis was the `cluster` from our `docker compose` github repository. But we only use one MongoDB and one Graylog node.

It is based on Docker to be able to reproduce all the steps consistently. Real installations will surely
differ. The steps can be also used to migrate a OS packages install.

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


