import React from 'react';
import Reflux from 'reflux';

import CurrentUserStore from 'stores/users/CurrentUserStore';
import DashboardStore from 'stores/dashboard/DashboardStore';

import Spinner from 'components/common/Spinner';

const ShowDashboardPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],

  componentDidMount() {
    DashboardStore.get(this.props.params.dashboardId)
      .then((dashboard) => {
        this.setState({dashboard: dashboard});
      });
  },
  render() {
    if (!this.state.dashboard) {
      return <Spinner />;
    }

    const dashboard = this.state.dashboard;
    const currentUser = this.state.currentUser;

    return (
      <div/>
    );
  }
});

export default ShowDashboardPage;
