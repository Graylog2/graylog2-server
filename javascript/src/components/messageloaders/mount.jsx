'use strict';

var React = require('react');
var LoaderTabs = require('./LoaderTabs');
var $ = require('jquery');

$('.react-message-loader-tabs').each(function() {
    React.render(<LoaderTabs/>, this);
});