'use strict';

var React = require('react/addons');
var LoaderTabs = require('./LoaderTabs');

$('.react-message-loader-tabs').each(function() {
    React.render(<LoaderTabs/>, this);
});