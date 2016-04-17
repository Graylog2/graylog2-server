import React from 'react';
import Reflux from 'reflux';
import DashboardListPage from 'components/dashboard/DashboardListPage';
import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const DashboardsPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
  render() {
    return (
      <DashboardListPage permissions={this.state.currentUser.permissions} />
    );
  }
});

export default DashboardsPage;
