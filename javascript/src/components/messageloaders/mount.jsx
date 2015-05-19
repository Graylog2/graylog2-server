'use strict';

var React = require('react/addons');
var LoaderTabs = require('./LoaderTabs');
var $ = require('jquery'); // excluded and shimed

$('.react-message-loader-tabs').each(function() {
    React.render(<LoaderTabs/>, this);
});