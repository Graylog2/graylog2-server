import React from 'react';
import PropTypes from 'prop-types';
import { LinkContainer } from 'react-router-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Col, DropdownButton, MenuItem, Row, Button } from 'components/graylog';
import {
  EmptyEntity,
  EntityList,
  EntityListItem,
  IfPermitted,
  PaginatedList,
  SearchForm,
  Spinner,
} from 'components/common';
import Routes from 'routing/Routes';

import styles from './EventNotifications.css';

class EventNotifications extends React.Component {
  static propTypes = {
    notifications: PropTypes.array.isRequired,
    pagination: PropTypes.object.isRequired,
    query: PropTypes.string.isRequired,
    testResult: PropTypes.shape({
      isLoading: PropTypes.bool,
      id: PropTypes.string,
      error: PropTypes.bool,
      message: PropTypes.string,
    }).isRequired,
    onPageChange: PropTypes.func.isRequired,
    onQueryChange: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
    onTest: PropTypes.func.isRequired,
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
    return PluginStore.exports('eventNotificationTypes').find((n) => n.type === type) || {};
  };

  formatNotification = (notifications) => {
    const { testResult } = this.props;

    return notifications.map((notification) => {
      const isTestLoading = testResult.id === notification.id && testResult.isLoading;
      const actions = this.formatActions(notification, isTestLoading);

      const plugin = this.getNotificationPlugin(notification.config.type);
      const content = testResult.id === notification.id ? (
        <Col md={12}>
          {testResult.isLoading ? (
            <Spinner text="Testing Notification..." />
          ) : (
            <p className={testResult.error ? 'text-danger' : 'text-success'}>
              <b>{testResult.error ? 'Error' : 'Success'}:</b> {testResult.message}
            </p>
          )}
        </Col>
      ) : null;

      return (
        <EntityListItem key={`event-definition-${notification.id}`}
                        title={notification.title}
                        titleSuffix={plugin.displayName || notification.config.type}
                        description={notification.description || <em>No description given</em>}
                        actions={actions}
                        contentRow={content} />
      );
    });
  };

  formatActions(notification, isTestLoading) {
    const { onDelete, onTest } = this.props;
    return (
      <>
        <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.edit(notification.id)}>
          <IfPermitted permissions={`eventnotifications:edit:${notification.id}`}>
            <Button bsStyle="info">Edit</Button>
          </IfPermitted>
        </LinkContainer>
        <IfPermitted permissions={[`eventnotifications:edit:${notification.id}`, `eventnotifications:delete:${notification.id}`]}>
          <DropdownButton id={`more-dropdown-${notification.id}`} title="More" pullRight>
            <IfPermitted permissions={`eventnotifications:edit:${notification.id}`}>
              <MenuItem disabled={isTestLoading} onClick={onTest(notification)}>
                {isTestLoading ? 'Testing...' : 'Test Notification'}
              </MenuItem>
            </IfPermitted>
            <MenuItem divider />
            <IfPermitted permissions={`eventnotifications:delete:${notification.id}`}>
              <MenuItem onClick={onDelete(notification)}>Delete</MenuItem>
            </IfPermitted>
          </DropdownButton>
        </IfPermitted>
      </>
    );
  }

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
