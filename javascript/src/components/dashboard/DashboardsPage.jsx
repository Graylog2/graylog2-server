import React from 'react';
import DashboardListPage from "components/dashboard/DashboardListPage";

const DashboardsPage = React.createClass({
  render() {
    return (
      <DashboardListPage permissions={["*"]} />
    );
  }
});

export default DashboardsPage;
