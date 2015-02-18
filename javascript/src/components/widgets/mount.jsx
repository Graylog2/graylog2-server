'use strict';

var React = require('react/addons');
var CountWidget = require('./CountWidget');
var HistogramWidget = require('./HistogramWidget');

var $ = require('jquery');
$('.react-widget').each(function() {
    var type = $(this).data('widget-type');
    var dashboardId = $(this).data('dashboard-id');
    var widgetId = $(this).data('widget-id');
    switch(type) {
        case 'count':
            React.render(<CountWidget dashboardId={dashboardId} widgetId={widgetId}/>, this);
            break;
        case 'histogram':
            React.render(<HistogramWidget dashboardId={dashboardId} widgetId={widgetId}/>, this);
            break;
        default:
            console.log("Invalid widget type");
    }
});
