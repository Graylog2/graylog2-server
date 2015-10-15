import React from 'react';
import Reflux from 'reflux';
import DashboardListPage from 'components/dashboard/DashboardListPage';
import CurrentUserStore from 'stores/users/CurrentUserStore'

const DashboardsPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
  render() {
    return (
      <DashboardListPage permissions={this.state.currentUser.permissions} />
    );
  }
});

export default DashboardsPage;
