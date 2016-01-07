Graylog Web Plugin Archetype
============================

This module is supposed to help with developing web interface plugins for [Graylog](http://www.graylog.org). It provides classes which help generating a webpack configuration, defining the plugin manifest (metadata as well as entities which are provided to the web interface) as well as the `PluginStore` class which is used for registering plugins to make them accessible for the web interface.

This module is supposed to be used in conjunction with the maven archetype which can be found [here](https://github.com/Graylog2/graylog-plugin-archetype).

## Installation

`npm install --save graylog-web-plugin`

## Usage

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
const ENTRY_PATH = path.resolve(ROOT_PATH, 'src/index.jsx);

module.exports = new PluginWebpackConfig('my.fully.qualified.plugin.classname', {build_path: BUILD_PATH, entry_path: ENTRY_PATH});
```

The third (optional) parameter for the `PluginWebpackConfig` constructor is an object which is merged into the generated webpack config, so you can add/overwrite any part of the generated configuration.

## Contributing

Feel free to contribute to this collection of helpers by forking the repository and submitting a pull request. Thanks!

## Release History

* 0.0.13 First actually usable version
