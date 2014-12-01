'use strict';

var React = require('react/addons');
var TrendConfigurationModal = require('./TrendConfigurationModal');

var dialogConfigurationDiv = document.getElementById('react-dashboard-widget-configuration-dialog');
if (dialogConfigurationDiv) {
    var component = React.render(<TrendConfigurationModal />, dialogConfigurationDiv);
    // XXX: to make it accessible from jquery based code
    if (window) {
        window.trendDialogConfiguration = component;
    }
}

var $ = require('jquery');
var EditDashboardModal = require('./EditDashboardModal');

$('.react-edit-dashboard').each(function() {
    var id = this.getAttribute('data-dashboard-id');
    var title = this.getAttribute('data-dashboard-title');
    var description = this.getAttribute('data-dashboard-description');

    React.render(<EditDashboardModal id={id} title={title} description={description}/>, this);
});
