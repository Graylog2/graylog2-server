'use strict';

var React = require('react/addons');
var Widget = require('./Widget');

var $ = require('jquery');
$('.react-widget').each(function() {
    var dashboardId = $(this).data('dashboard-id');
    var widgetId = $(this).data('widget-id');
    React.render(<Widget dashboardId={dashboardId} widgetId={widgetId}/>, this);
});
