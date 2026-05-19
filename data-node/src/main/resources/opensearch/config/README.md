## Opensearch configuration
This readme describes how the opensearch configuration files are handled and copied to the runtime location.

There are two approaches - one for common config files and one for version specific files.

### Common
this is a directory in main/resources/opensearch/config/common that holds all the common initial configuration files needed by the opensearch
we manage this directory in git. Generally we assume that this is a read-only location and we need to copy
its content to a read-write location for the managed opensearch process.
This copy happens during each opensearch process start and will override any files that already exist
from previous runs.

### Version specific
this is a subdirectory in main/resources/opensearch/config/, named with semver version of the specific opensearch
version. For 2.19.5, we'll copy files from main/resources/opensearch/config/2.19.5 only.


## Distribution properties
If there are some version-specific names, roles, config options, they can be configured in the distribution.properties
file for each version. These are then available through OpensearchDistribution class.
