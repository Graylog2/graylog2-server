import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { DocumentTitle } from 'components/common';
import DashboardListPage from 'components/dashboard/DashboardListPage';
import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const DashboardsPage = createReactClass({
  displayName: 'DashboardsPage',
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
