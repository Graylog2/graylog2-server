'use strict';

var React = require('react');
var ReactDOM = require('react-dom');
var ConfigurationBundles = require('./ConfigurationBundles');

var configurationBundles = document.getElementById('react-configuration-bundles');
if (configurationBundles) {
    ReactDOM.render(<ConfigurationBundles />, configurationBundles);
}
