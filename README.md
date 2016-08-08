Graylog Web Plugin Archetype
============================

This module is supposed to help with developing web interface plugins for [Graylog](http://www.graylog.org). It provides classes which help generating a webpack configuration, defining the plugin manifest (metadata as well as entities which are provided to the web interface) as well as the `PluginStore` class which is used for registering plugins to make them accessible for the web interface.

This module is supposed to be used in conjunction with the maven archetype which can be found [here](https://github.com/Graylog2/graylog-plugin-archetype) and requires a checkout of the [Graylog server repository](https://github.com/Graylog2/graylog2-server) in some place.

## Installation

`npm install --save graylog-web-plugin`

## Usage

```
$ npm init
[...]
$ npm --save-dev graylog-web-plugin
```

Create a file named `build.config.js` in your plugin directory, looking like this:

```
module.exports = {
  web_src_path: '<path to your Graylog git repository>',
};
```

and use a webpack.config.js like this: 

```
const PluginWebpackConfig = require('graylog-web-plugin').PluginWebpackConfig;
const loadBuildConfig = require('graylog-web-plugin').loadBuildConfig;
const path = require('path');
const buildConfig = loadBuildConfig(path.resolve(__dirname, './build.config'));

module.exports = new PluginWebpackConfig('my.fully.qualified.plugin.classname', buildConfig, {
  // Here goes your additional webpack configuration.
});
```

The third (optional) parameter for the `PluginWebpackConfig` constructor is an object which is merged into the generated webpack config, so you can add/overwrite any part of the generated configuration.

## Contributing

Feel free to contribute to this collection of helpers by forking the repository and submitting a pull request. Thanks!

## Release a new version

    $ npm version [<newversion> | major | minor | patch]
    $ git push origin master && git push --tags
    $ npm publish

## Release History
* 0.0.21 Add optimizations for production builds.
* 0.0.20 Add plugin fqdn as prefix of built `module.json`.
* 0.0.19 Add webpack config to compile typescript code in plugins.
* 0.0.18 Removing now unneeded shared bundle.
* 0.0.17 Bugfix, using absolute filename for build config.
* 0.0.16 Adding build config file including option specifying location of Graylog server repo checkout.
* 0.0.15 Bugfix, add missing import
* 0.0.14 Add shared and vendor bundle to generated webpack config
* 0.0.13 First actually usable version
