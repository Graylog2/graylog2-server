const fs = require('fs');
const buildConfig = require('./build.config');

const packageJson = JSON.parse(fs.readFileSync('package.json'));
const webSrcPrefix = buildConfig.web_src_path;
const { moduleDirectories } = packageJson.jest;

const jestConfig = Object.assign({}, packageJson.jest, {
  moduleDirectories: [].concat(moduleDirectories, [webSrcPrefix + '/src', webSrcPrefix + '/test']),
});
module.exports = jestConfig;

