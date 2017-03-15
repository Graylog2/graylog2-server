import React from 'react';

import { AlertNotification } from 'components/alertnotifications';
import { EntityList, PaginatedList } from 'components/common';

const AlertNotificationsList = React.createClass({
  propTypes: {
    alertNotifications: React.PropTypes.array.isRequired,
    streams: React.PropTypes.array.isRequired,
    onNotificationUpdate: React.PropTypes.func,
    onNotificationDelete: React.PropTypes.func,
  },

  getInitialState() {
    return {
      currentPage: 0,
    };
  },

  PAGE_SIZE: 10,

  _onChangePaginatedList(currentPage) {
    this.setState({ currentPage: currentPage - 1 });
  },

  _paginatedNotifications() {
    return this.props.alertNotifications.slice(this.state.currentPage * this.PAGE_SIZE, (this.state.currentPage + 1) * this.PAGE_SIZE);
  },

  _formatNotification(notification) {
    const stream = this.props.streams.find(s => s.id === notification.stream_id);
    return (
      <AlertNotification key={notification.id} alertNotification={notification} stream={stream}
                         onNotificationUpdate={this.props.onNotificationUpdate}
                         onNotificationDelete={this.props.onNotificationDelete} />
    );
  },

  render() {
    const notifications = this.props.alertNotifications;

    return (
      <PaginatedList totalItems={notifications.length} onChange={this._onChangePaginatedList}
                     showPageSizeSelect={false} pageSize={this.PAGE_SIZE}>
        <EntityList bsNoItemsStyle="info"
                    noItemsText="There are no configured notifications."
                    items={this._paginatedNotifications().map(notification => this._formatNotification(notification))} />
      </PaginatedList>
    );
  },
});

export default AlertNotificationsList;
