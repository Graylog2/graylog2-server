Upgrading to Graylog 5.1.x
==========================

## New Functionality

## Index Default Configuration
Support for configuring system index initialization defaults, and ongoing in-database index set defaults has been added.
Please see the corresponding change log entry [pr-13018.toml](https://github.com/Graylog2/graylog2-server/blob/master/changelog/5.1.0/pr-13018.toml)
for  important details to be aware of before upgrading to Graylog `5.1.x`.

# API Changes
The following Java Code API changes have been made.

| File/method                                  | Description                                                                                                 |
|----------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `IndexSetValidator#validateRefreshInterval`  | The method argument have changed from `IndexSetConfig` to `Duration`                                        |
| `IndexSetValidator#validateRetentionPeriod`  | The method argument have changed from `IndexSetConfig` to `RotationStrategyConfig, RetentionStrategyConfig` |
| `ElasticsearchConfiguration#getIndexPrefix`  | The method name has changed to `getDefaultIndexPrefix`                                                      |
| `ElasticsearchConfiguration#getTemplateName` | The method name has changed to `getDefaultIndexTemplateName`                                                |

All previously deprecated index set configuration properties in `org.graylog2.configuration.ElasticsearchConfiguration`
have been un-deprecated, as Graylog intends to maintain them going forward. Please see the corresponding change log
[pr-13018.toml](https://github.com/Graylog2/graylog2-server/blob/master/changelog/5.1.0/pr-13018.toml) for more
information.

## Breaking Changes
