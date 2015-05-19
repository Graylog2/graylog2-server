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
var EditDashboardModalTrigger = require('./EditDashboardModalTrigger');

$('.react-edit-dashboard').each(function() {
    var id = this.getAttribute('data-dashboard-id');
    var title = this.getAttribute('data-dashboard-title');
    var description = this.getAttribute('data-dashboard-description');
    var buttonClass = this.getAttribute('data-button-class');
    var content = this.innerHTML;

    var component = (
        <EditDashboardModalTrigger id={id} action="edit" title={title} description={description} buttonClass={buttonClass}>
            {content}
        </EditDashboardModalTrigger>
    );

    React.render(component, this);
});
