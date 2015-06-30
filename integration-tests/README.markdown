# Integration Tests

__The integration tests need explicit activation by setting the ``skip.integration.tests``
tunable to *false*.__
All tests require a running Graylog server to run against, which is not bootstrapped by maven but supposedly
by the environment the integration tests are running on. Therefore, the tests have a number of tunables with (hopefully)
reasonable defaults. These are:

| Property               | Default | Description |
|:-----------------------|:--------|:------------|
| skip.integration.tests | ``false``   | Enables the integration tests. (Disabled per default) |
| gl2.port | ``12900`` | Tcp port of the graylog server. |
| gl2.baseuri | ``"http://localhost:12900"`` | Uri base of the graylog server. Port is overriden by gl2.port if specified. |
| gl2.admin_user | ``admin`` | Username used for authenticating against server for authenticated requests. |
| gl2.admin_password | ``admin`` | Password used for authenticating against server for authenticated requests. |
| mongodb.host | ``"localhost"`` | Hostname of MongoDB server used for seeding tests that require a preseeded database. |
| mongodb.port | ``27017`` | Tcp port of MongoDB server. |
| mongodb.database | ``"graylog_test"`` | Default database name for MongoDB. Tests are able to override this. |
| es.host | ``"localhost"`` | Hostname used for connecting to elasticsearch cluster during preseeding. |
| es.cluster.name | ``"graylog_test"`` | elasticsearch cluster name used during connecting. |
| es.port | ``9300`` | elasticsearch port used during connecting. |

All tunables are passed as java system properties (i.e. by adding ``-Dmongodb.database=integration`` to the mvn command
to define a different default MongoDB database).