const fs = require('fs');
const path = require('path');

module.exports = function loadBuildConfig(filename) {
  try {
    const buildConfig = require(path.resolve(module.parent.parent.filename, '../', filename));
    if (!buildConfig.web_src_path || !fs.lstatSync(buildConfig.web_src_path).isDirectory()) {
      console.error("Path to graylog web interface sources is not defined, does not exist or is not a directory: (", buildConfig.web_src_path, ').');
      console.error("Please configure it in a file named `build.config.js` before trying to build the plugin.");
      // TODO: add link to documentation
      console.error("For further information please check http://docs.graylog.org/PLACEHOLDER");
      process.exit(-1);
    }
    return buildConfig;
  } catch (e) {
    console.error("It seems like there is no readable build.config.js file: ", e);
  }
  process.exit(-1);
}
