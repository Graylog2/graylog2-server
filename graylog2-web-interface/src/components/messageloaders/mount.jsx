'use strict';

var React = require('react');
var ReactDOM = require('react-dom');
var LoaderTabs = require('./LoaderTabs');
var $ = require('jquery');

$('.react-message-loader-tabs').each(function() {
    ReactDOM.render(<LoaderTabs/>, this);
});
