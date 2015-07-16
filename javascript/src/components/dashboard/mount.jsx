'use strict';

var React = require('react');
var TrendConfigurationModal = require('./TrendConfigurationModal');
var component;

var dialogConfigurationDiv = document.getElementById('react-dashboard-widget-configuration-dialog');
if (dialogConfigurationDiv) {
    component = React.render(<TrendConfigurationModal />, dialogConfigurationDiv);
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

    component = (
        <EditDashboardModalTrigger id={id} action="edit" title={title} description={description} buttonClass={buttonClass}>
            {content}
        </EditDashboardModalTrigger>
    );

    React.render(component, this);
});

var createDashboardElements = document.getElementsByClassName('react-create-dashboard');
var reloadOnDashboardCreated = () => document.location.reload();

for (var i = 0; i < createDashboardElements.length; i++) {
    var element = createDashboardElements[i];
    var content = element.innerHTML;
    var buttonClass = element.getAttribute('data-button-class');

    component = (
        <EditDashboardModalTrigger action='create' buttonClass={buttonClass} onSaved={reloadOnDashboardCreated}>
            {content}
        </EditDashboardModalTrigger>
    );

    React.render(component, element);
}