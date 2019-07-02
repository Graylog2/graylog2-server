import React from 'react';
import PropTypes from 'prop-types';
import { Button, Col, DropdownButton, MenuItem, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { EmptyEntity, EntityList, EntityListItem, PaginatedList } from 'components/common';

import Routes from 'routing/Routes';

class EventNotifications extends React.Component {
  static propTypes = {
    notifications: PropTypes.array.isRequired,
    pagination: PropTypes.object.isRequired,
    onPageChange: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
  };

  renderEmptyContent = () => {
    return (
      <Row>
        <Col md={4} mdOffset={4}>
          <EmptyEntity>
            <p>
              Configure Event Notifications that can alert you when an Event occurs. You can also use Notifications
              to integrate Graylog Alerts with an external alerting system you use.
            </p>
            <LinkContainer to={Routes.NEXT_ALERTS.NOTIFICATIONS.CREATE}>
              <Button bsStyle="success">Get Started!</Button>
            </LinkContainer>
          </EmptyEntity>
        </Col>
      </Row>
    );
  };

  getNotificationPlugin = (type) => {
    if (type === undefined) {
      return {};
    }
    return PluginStore.exports('eventNotificationTypes').find(n => n.type === type);
  };

  formatNotification = (notifications) => {
    const { onDelete } = this.props;

    return notifications.map((notification) => {
      const actions = (
        <React.Fragment>
          <LinkContainer to={Routes.NEXT_ALERTS.NOTIFICATIONS.edit(notification.id)}>
            <Button bsStyle="info">Edit</Button>
          </LinkContainer>
          <DropdownButton id={`more-dropdown-${notification.id}`} title="More" pullRight>
            <MenuItem onClick={onDelete(notification)}>Delete</MenuItem>
          </DropdownButton>
        </React.Fragment>
      );

      const plugin = this.getNotificationPlugin(notification.config.type);

      return (
        <EntityListItem key={`event-definition-${notification.id}`}
                        title={notification.title}
                        titleSuffix={plugin.displayName || notification.config.type}
                        description={notification.description}
                        actions={actions} />
      );
    });
  };

  render() {
    const { notifications, pagination, onPageChange } = this.props;

    if (notifications.length === 0) {
      return this.renderEmptyContent();
    }

    return (
      <React.Fragment>
        <Row>
          <Col md={12}>
            <div className="pull-right">
              <LinkContainer to={Routes.NEXT_ALERTS.NOTIFICATIONS.CREATE}>
                <Button bsStyle="success">Create Notification</Button>
              </LinkContainer>
            </div>
          </Col>
        </Row>
        <Row>
          <Col md={12}>
            <PaginatedList activePage={pagination.page}
                           pageSize={pagination.pageSize}
                           pageSizes={[10, 25, 50]}
                           totalItems={pagination.total}
                           onChange={onPageChange}>
              <EntityList items={this.formatNotification(notifications)} />
            </PaginatedList>
          </Col>
        </Row>
      </React.Fragment>
    );
  }
}

export default EventNotifications;
