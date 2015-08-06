'use strict';

var React = require('react');
var $ = require('jquery');

var TrendConfigurationModal = require('./TrendConfigurationModal');
var EditDashboardModalTrigger = require('./EditDashboardModalTrigger');
var DashboardListPage = require('./DashboardListPage');

var component;

var dialogConfigurationDiv = document.getElementById('react-dashboard-widget-configuration-dialog');
if (dialogConfigurationDiv) {
    component = React.render(<TrendConfigurationModal />, dialogConfigurationDiv);
    // XXX: to make it accessible from jquery based code
    if (window) {
        window.trendDialogConfiguration = component;
    }
}

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

var dashboardListPage = document.getElementById('react-dashboard-list-page');
if (dashboardListPage) {
    var permissions = JSON.parse(dashboardListPage.getAttribute('data-permissions'));
    var username = dashboardListPage.getAttribute('data-user-name');
    component = <DashboardListPage permissions={permissions} username={username}/>;

    React.render(component, dashboardListPage);
}
