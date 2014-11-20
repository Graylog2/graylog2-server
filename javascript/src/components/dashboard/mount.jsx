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
