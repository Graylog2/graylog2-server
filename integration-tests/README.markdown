# Integration Tests

__The integration tests need explicit activation by setting the ``skip.integration.tests`` tunable to *false*.__

All tests require a running Graylog server to run against, which is not bootstrapped by Maven but supposedly
by the environment the integration tests are running on. Therefore, the tests have a number of tunables with (hopefully)
reasonable defaults. These are:

| Property                   | Default                    | Description                                                                          |
|:---------------------------|:---------------------------|:-------------------------------------------------------------------------------------|
| ``skip.integration.tests`` | ``true``                   | Enables the integration tests. (Disabled per default)                                |
| ``gl.port``                | ``12900``                  | TCP port of the Graylog server.                                                      |
| ``gl.baseuri``             | ``http://localhost:12900`` | Uri base of the Graylog server. Port is overriden by ``gl.port`` if specified.       |
| ``gl.admin_user``          | ``admin``                  | Username used for authenticating against server for authenticated requests.          |
| ``gl.admin_password``      | ``admin``                  | Password used for authenticating against server for authenticated requests.          |
| ``mongodb.host``           | ``localhost``              | Hostname of MongoDB server used for seeding tests that require a preseeded database. |
| ``mongodb.port``           | ``27017``                  | TCP port of MongoDB server.                                                          |
| ``mongodb.database``       | ``graylog_test``           | Default database name for MongoDB. Tests are able to override this.                  |
| ``es.host``                | ``localhost``              | Hostname used for connecting to Elasticsearch cluster during preseeding.             |
| ``es.cluster.name``        | ``graylog_test``           | Elasticsearch cluster name used during connecting.                                   |
| ``es.port``                | ``9300``                   | Elasticsearch port used during connecting.                                           |

All tunables are passed as Java system properties (i.e. by adding ``-Dmongodb.database=integration`` to the Maven command
to define a different default MongoDB database).
