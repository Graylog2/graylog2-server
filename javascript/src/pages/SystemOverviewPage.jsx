import React from 'react';

import NotificationsList from 'components/notifications/NotificationsList';

const SystemOverviewPage = React.createClass({
  render() {
    return (
      <span>
        <NotificationsList />
      </span>
    );
  }
});

export default SystemOverviewPage;
