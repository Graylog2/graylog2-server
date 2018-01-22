import React from 'react';
import PropTypes from 'prop-types';
import { inject, observer } from 'mobx-react';
import { DocumentTitle } from 'components/common';
import DashboardListPage from 'components/dashboard/DashboardListPage';
import StoreProvider from 'injection/StoreProvider';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const DashboardsPage = React.createClass({
  propTypes: {
    currentUser: PropTypes.object.isRequired,
  },

  render() {
    return (
      <DocumentTitle title="Dashboards">
        <DashboardListPage permissions={this.props.currentUser.permissions} />
      </DocumentTitle>
    );
  },
});

export default inject(() => ({
  currentUser: CurrentUserStore.currentUser,
}))(observer(DashboardsPage));
