# Graylog Web Plugin Archetype

This module is supposed to help with developing web interface plugins for [Graylog](http://www.graylog.org). By using this module, all you have to do is to use the following steps:

```
$ npm init
[...]
$ npm --save-dev graylog-web-plugin
```

and use a webpack.config.js like this: 

```
const path = require('path');
const PluginWebpackConfig = require('graylog-web-plugin').PluginWebpackConfig;

const ROOT_PATH = path.resolve(__dirname);
const BUILD_PATH = path.resolve(ROOT_PATH, 'build');

module.exports = new PluginWebpackConfig('my.fully.qualified.plugin.classname', BUILD_PATH);
```
