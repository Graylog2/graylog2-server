const fs = require('fs');

module.exports = function loadBuildConfig(filename) {
  try {
    // eslint-disable-next-line global-require
    const buildConfig = require(filename);
    if (!buildConfig.web_src_path || !fs.lstatSync(buildConfig.web_src_path).isDirectory()) {
      /* eslint-disable no-console */
      console.error('Path to graylog web interface sources is not defined, does not exist or is not a directory: (', buildConfig.web_src_path, ').');
      console.error('Please configure it in a file named `build.config.js` before trying to build the plugin.');
      // TODO: add link to documentation
      console.error('For further information please check http://docs.graylog.org/PLACEHOLDER');
      process.exit(-1);
      /* eslint-enable no-console */
    }
    return buildConfig;
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error('It seems like there is no readable build.config.js file: ', e);
  }
  process.exit(-1);
  return undefined;
};
