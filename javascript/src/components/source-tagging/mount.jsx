'use strict';

var React = require('react');
var ConfigurationBundles = require('./ConfigurationBundles');

var configurationBundles = document.getElementById('react-configuration-bundles');
if (configurationBundles) {
    React.render(<ConfigurationBundles />, configurationBundles);
}
