import React from 'react';
import Reflux from 'reflux';
import { DocumentTitle } from 'components/common';
import DashboardListPage from 'components/dashboard/DashboardListPage';
import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const DashboardsPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
  render() {
    return (
      <DocumentTitle title="Dashboards">
        <DashboardListPage permissions={this.state.currentUser.permissions} />
      </DocumentTitle>
    );
  },
});

export default DashboardsPage;
