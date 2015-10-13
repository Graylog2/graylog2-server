'use strict';

var React = require('react');
var ReactDOM = require('react-dom');
var Widget = require('./Widget');

var $ = require('jquery');
$('.react-widget').each(function() {
    var dashboardId = $(this).data('dashboard-id');
    var widgetId = $(this).data('widget-id');
    ReactDOM.render(<Widget dashboardId={dashboardId} widgetId={widgetId}/>, this);
});
