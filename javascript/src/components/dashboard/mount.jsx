const React = require('react');
const $ = require('jquery');

const TrendConfigurationModal = require('./TrendConfigurationModal');
const EditDashboardModalTrigger = require('./EditDashboardModalTrigger');
const DashboardListPage = require('./DashboardListPage');

let component;

const dialogConfigurationDiv = document.getElementById('react-dashboard-widget-configuration-dialog');
if (dialogConfigurationDiv) {
  component = React.render(<TrendConfigurationModal />, dialogConfigurationDiv);
  // XXX: to make it accessible from jquery based code
  if (window) {
    window.trendDialogConfiguration = component;
  }
}

$('.react-edit-dashboard').each(function () {
  const id = this.getAttribute('data-dashboard-id');
  const title = this.getAttribute('data-dashboard-title');
  const description = this.getAttribute('data-dashboard-description');
  const buttonClass = this.getAttribute('data-button-class');
  const content = this.innerHTML;

  component = (
    <EditDashboardModalTrigger id={id} action="edit" title={title} description={description} buttonClass={buttonClass}>
      {content}
    </EditDashboardModalTrigger>
  );

  React.render(component, this);
});

const dashboardListPage = document.getElementById('react-dashboard-list-page');
if (dashboardListPage) {
  const permissions = JSON.parse(dashboardListPage.getAttribute('data-permissions'));
  const username = dashboardListPage.getAttribute('data-user-name');
  component = <DashboardListPage permissions={permissions} username={username}/>;

  React.render(component, dashboardListPage);
}
