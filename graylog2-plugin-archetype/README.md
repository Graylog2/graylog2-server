Graylog2 Maven Plugin Archetype
===============================

```
$ mvn archetype:generate -Dfilter=org.graylog2:
[...]
Choose archetype:
1: local -> org.graylog2:graylog2-plugin-archetype
Choose a number or apply filter (format: [groupId:]artifactId, case sensitive contains): : 1
Define value for property 'groupId': : org.graylog2
Define value for property 'artifactId': : plugin-output-null
[INFO] Using property: version = 1.0.0-SNAPSHOT
Define value for property 'package':  org.graylog2: :
Define value for property 'pluginClassName':  : : NullOutput
Confirm properties configuration:
groupId: org.graylog2
artifactId: plugin-output-null
version: 1.0.0-SNAPSHOT
package: org.graylog2
pluginClassName: NullOutput
 Y: : y
[INFO] ----------------------------------------------------------------------------
[INFO] Using following parameters for creating project from Archetype: graylog2-plugin-archetype:1.0.0-SNAPSHOT
[INFO] ----------------------------------------------------------------------------
[INFO] Parameter: groupId, Value: org.graylog2
[INFO] Parameter: artifactId, Value: plugin-output-null
[INFO] Parameter: version, Value: 1.0.0-SNAPSHOT
[INFO] Parameter: package, Value: org.graylog2
[INFO] Parameter: packageInPathFormat, Value: org/graylog2
[INFO] Parameter: package, Value: org.graylog2
[INFO] Parameter: version, Value: 1.0.0-SNAPSHOT
[INFO] Parameter: groupId, Value: org.graylog2
[INFO] Parameter: pluginClassName, Value: NullOutput
[INFO] Parameter: artifactId, Value: plugin-output-null
[INFO] project created from Archetype in dir: plugin-output-null
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```
