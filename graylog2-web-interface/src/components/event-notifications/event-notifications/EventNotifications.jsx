import React from 'react';
import PropTypes from 'prop-types';
import { Button, Col, DropdownButton, MenuItem, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { EmptyEntity, EntityList, EntityListItem, IfPermitted, PaginatedList, SearchForm } from 'components/common';

import Routes from 'routing/Routes';

import styles from './EventNotifications.css';

class EventNotifications extends React.Component {
  static propTypes = {
    notifications: PropTypes.array.isRequired,
    pagination: PropTypes.object.isRequired,
    query: PropTypes.string.isRequired,
    onPageChange: PropTypes.func.isRequired,
    onQueryChange: PropTypes.func.isRequired,
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
            <IfPermitted permissions="eventnotifications:create">
              <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.CREATE}>
                <Button bsStyle="success">Get Started!</Button>
              </LinkContainer>
            </IfPermitted>
          </EmptyEntity>
        </Col>
      </Row>
    );
  };

  getNotificationPlugin = (type) => {
    if (type === undefined) {
      return {};
    }
    return PluginStore.exports('eventNotificationTypes').find(n => n.type === type) || {};
  };

  formatNotification = (notifications) => {
    const { onDelete } = this.props;

    return notifications.map((notification) => {
      const actions = (
        <React.Fragment>
          <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.edit(notification.id)}>
            <IfPermitted permissions={`eventnotifications:edit:${notification.id}`}>
              <Button bsStyle="info">Edit</Button>
            </IfPermitted>
          </LinkContainer>
          <IfPermitted permissions={`eventnotifications:delete:${notification.id}`}>
            <DropdownButton id={`more-dropdown-${notification.id}`} title="More" pullRight>
              <MenuItem onClick={onDelete(notification)}>Delete</MenuItem>
            </DropdownButton>
          </IfPermitted>
        </React.Fragment>
      );

      const plugin = this.getNotificationPlugin(notification.config.type);

      return (
        <EntityListItem key={`event-definition-${notification.id}`}
                        title={notification.title}
                        titleSuffix={plugin.displayName || notification.config.type}
                        description={notification.description || <em>No description given</em>}
                        actions={actions} />
      );
    });
  };

  render() {
    const { notifications, pagination, query, onPageChange, onQueryChange } = this.props;

    if (pagination.grandTotal === 0) {
      return this.renderEmptyContent();
    }

    return (
      <Row>
        <Col md={12}>
          <SearchForm query={query}
                      onSearch={onQueryChange}
                      onReset={onQueryChange}
                      searchButtonLabel="Find"
                      placeholder="Find Notifications"
                      wrapperClass={styles.inline}
                      queryWidth={200}
                      topMargin={0}
                      useLoadingState>
            <IfPermitted permissions="eventnotifications:create">
              <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.CREATE}>
                <Button bsStyle="success" className={styles.createButton}>Create Notification</Button>
              </LinkContainer>
            </IfPermitted>
          </SearchForm>

          <PaginatedList activePage={pagination.page}
                         pageSize={pagination.pageSize}
                         pageSizes={[10, 25, 50]}
                         totalItems={pagination.total}
                         onChange={onPageChange}>
            <div className={styles.notificationList}>
              <EntityList items={this.formatNotification(notifications)} />
            </div>
          </PaginatedList>
        </Col>
      </Row>
    );
  }
}

export default EventNotifications;
