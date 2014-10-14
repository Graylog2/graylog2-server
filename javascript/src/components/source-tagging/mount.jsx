'use strict';

var React = require('react/addons');
var ConfigurationBundles = require('./ConfigurationBundles');

var configurationBundles = document.getElementById('react-configuration-bundles');
if (configurationBundles) {
    React.renderComponent(<ConfigurationBundles />, configurationBundles);
}
