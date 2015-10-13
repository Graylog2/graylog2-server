import React from 'react';
import ReactDOM from 'react-dom';
import $ from 'jquery';

import EditDashboardModalTrigger from './EditDashboardModalTrigger';
import DashboardListPage from './DashboardListPage';

let component;

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

  ReactDOM.render(component, this);
});

const dashboardListPage = document.getElementById('react-dashboard-list-page');
if (dashboardListPage) {
  const permissions = JSON.parse(dashboardListPage.getAttribute('data-permissions'));
  const username = dashboardListPage.getAttribute('data-user-name');
  component = <DashboardListPage permissions={permissions} username={username}/>;

  ReactDOM.render(component, dashboardListPage);
}
