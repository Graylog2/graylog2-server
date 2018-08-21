
## Tools

We'll use:

 * https://httpie.org/
 * https://stedolan.github.io/jq/
 
## Preparations

* Deploy Graylog 2.5.0-SNAPSHOT
* Create test directory `mkdir /tmp/graylog_upgrade` and cd there. Everything happens here.
* Unpack elasticsearch versions (latest of their series)
  * 2.4.6 `https://download.elastic.co/elasticsearch/release/org/elasticsearch/distribution/tar/elasticsearch/2.4.6/elasticsearch-2.4.6.tar.gz`
  * 5.6.10 `https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-5.6.10.tar.gz`
  * 6.3.2 `https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.3.2.tar.gz`
* Create symlinks for each version (to minimize typing and tab-completion frustration)
  * `ln -s elasticsearch-2.4.6 2`
  * `ln -s elasticsearch-5.6.10 5`
  * `ln -s elasticsearch-6.3.2 6`
* Configure elasticsearch versions (see sections below)

## Upgrade tests

* Start ES 2.4: `./2/bin/elasticsearch`
* Start Graylog 2.5
 * Change Default Index Set rotation config to rotate every 20k messages
 * Start random HTTP input(s) to with enough load to trigger rotation regularly (20kmsg/s / 30msg/s ~ 10min per index)
* Stop 2.4 `Ctrl-C`
* Copy `24` directory to `56`: `cp -R 24 56`
* Start 5.6: `./5/bin/elasticsearch`
  * Watch deprecation log for index mapping warnings:
    ```
    [2018-08-21T15:11:31,062][WARN ][o.e.d.i.m.TypeParsers    ] Expected a boolean [true/false] for property [index] but got [not_analyzed]
    ```
* Wait for index rotation, new index should have new index mapping and settings:
  * Retrieve names of indices created prior to 5.x: 
    * `http 127.0.0.1:9200/graylog25_*/_settings | jq '[ path(.[] | select(.settings.index.version.created < "5000000"))[] ]'`:
       ```json
       [
        "graylog25_1",
        "graylog25_0"
        ]
       ```
    * These indices need to be reindexed in order to be able to start a 6.x cluster.
  * Check mapping: After an index rotation (manually or due to retention) the newly installed index mapping template should be used.
    * Verify by running:
    `http 127.0.0.1:9200/graylog25_*/_mapping | jq '[ path(.[] | select(.mappings.message.properties.gl2_source_input.type == "keyword"))[] ]'`
    ```json
    [
      "graylog25_9",
      "graylog25_6",
      "graylog25_4",
      "graylog25_2",
      "graylog25_7",
      "graylog25_3",
      "graylog25_10",
      "graylog25_8",
      "graylog25_5"
    ]
    ```
    * Cross check that the old indices still have the deprecated index mapping:
    `http 127.0.0.1:9200/graylog25_*/_mapping | jq '[ path(.[] | select(.mappings.message.properties.gl2_source_input.index == "not_analyzed"))[] ]'`
* Reindex old indices
  * TODO: this also requires a decision on how to integrate the new names into the relevant index sets and preventing premature retention cleaning 
* Stop 5.6: `Ctrl-C`
* Copy `56` data to `63`: `cp -R 56 63`
* Remove cluster name in data directory (or symlink)
  * `pushd 63; ln -s graylog_upgrade/nodes; popd`
  * OR `mv graylog_upgrade/nodes . ; rmdir graylog_upgrade`
* Start 6.3: `./6/bin/elasticsearch`
  * If we hadn't reindexed the the old indices, this would have failed due to incompatible 2.x indices:
  ```
  [2018-08-21T15:21:27,109][ERROR][o.e.g.GatewayMetaState   ] [ZdInArk] failed to read local state, exiting...
  java.lang.IllegalStateException: The index [[graylog25_1/8ILwepokQNSYmXp2c9K24A]] was created with version [2.4.6] but the minimum compatible version is [5.0.0]. It should be re-indexed in Elasticsearch 5.x before upgrading to 6.3.2.
  ...
  ```
* TODO

## Configurations

The idea is to copy the relevant data directory every time we perform an "upgrade", so we can inspect what the state was before and redo the upgrades more easily.
Thus the directory layout is (stripped of the irrelevant elastic tarball content):

```
kroepke@otter:~/local/elastic/graylog-upgrade$ tree -L 2
.
├── 2 -> elasticsearch-2.4.6
├── 24
│   ├── data
│   └── logs
├── 5 -> elasticsearch-5.6.10
├── 56
│   ├── data
│   └── logs
├── 6 -> elasticsearch-6.3.2
├── 63
│   ├── data
│   └── logs
├── elasticsearch-2.4.6
│   ├── bin
│   ├── config
│   └── [...]
├── elasticsearch-5.6.10
│   ├── bin
│   ├── config
│   └── [...]
└── elasticsearch-6.3.2
    ├── bin
    ├── config
    └── [...]
```

Note: You can probably skip the `network.host` if doing all of this on your local machine and not using Docker for `cerebro` or so (brave new world).


### 2.4

The mininmal config is:
```yaml
cluster.name: graylog_upgrade

path.data: /tmp/graylog-upgrade/24/data
path.logs: /tmp/graylog-upgrade/24/logs
```

### 5.6

Note: Both 5.6 and 6.3 require changing the `discovery.type` to avoid bootstrap checks refusing to start, when binding to a non-local address.

```yaml
cluster.name: graylog_upgrade

path.data: /tmp/graylog-upgrade/56/data
path.logs: /tmp/graylog-upgrade/56/logs
discovery.type: single-node
```

### 6.3
```yaml
cluster.name: graylog_upgrade

path.data: /tmp/graylog-upgrade/63/data
path.logs: /tmp/graylog-upgrade/63/logs
discovery.type: single-node
```

