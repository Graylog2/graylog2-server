/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import { LinkContainer, Link } from 'components/common/router';
import EntityShareModal from 'components/permissions/EntityShareModal';
import {
  EmptyEntity,
  EntityList,
  EntityListItem,
  ShareButton,
  IfPermitted,
  PaginatedList,
  SearchForm,
  Spinner,
  Icon,
  QueryHelper,
} from 'components/common';
import { Col, DropdownButton, MenuItem, Row, Button, DeleteMenuItem } from 'components/bootstrap';
import Routes from 'routing/Routes';

import styles from './EventNotifications.css';

export const PAGE_SIZES = [10, 25, 50];

const renderEmptyContent = () => (
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

const getNotificationPlugin = (type) => {
  if (type === undefined) {
    return undefined;
  }

  return PluginStore.exports('eventNotificationTypes').find((n) => n.type === type);
};

type EventNotificationsProps = {
  notifications: any[];
  pagination: any;
  query: string;
  testResult: {
    isLoading?: boolean;
    id?: string;
    error?: boolean;
    message?: string;
  };
  onPageChange: (...args: any[]) => void;
  onQueryChange: (...args: any[]) => void;
  onDelete: (...args: any[]) => (...args: any[]) => void;
  onTest: (...args: any[]) => (...args: any[]) => void;
};

class EventNotifications extends React.Component<EventNotificationsProps, {
  [key: string]: any;
}> {
  constructor(props) {
    super(props);

    this.state = {
      notificationToShare: undefined,
    };
  }

  formatNotification = (notifications, setNotificationToShare) => {
    const { testResult } = this.props;

    return notifications.map((notification) => {
      const isTestLoading = testResult.id === notification.id && testResult.isLoading;
      const actions = this.formatActions(notification, isTestLoading, setNotificationToShare);

      const plugin = getNotificationPlugin(notification.config.type);
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

      const title = <Link to={Routes.ALERTS.NOTIFICATIONS.show(notification.id)}>{notification.title}</Link>;

      return (
        <EntityListItem key={`event-definition-${notification.id}`}
                        title={title}
                        titleSuffix={plugin?.displayName || notification.config.type}
                        description={notification.description || <em>No description given</em>}
                        actions={actions}
                        contentRow={content} />
      );
    });
  };

  formatActions(notification, isTestLoading, setNotificationToShare) {
    const { onDelete, onTest } = this.props;

    return (
      <ButtonToolbar>
        <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.edit(notification.id)}>
          <IfPermitted permissions={`eventnotifications:edit:${notification.id}`}>
            <Button>
              <Icon name="edit_square" /> Edit
            </Button>
          </IfPermitted>
        </LinkContainer>
        <ShareButton entityType="notification" entityId={notification.id} onClick={() => setNotificationToShare(notification)} />
        <IfPermitted permissions={[`eventnotifications:edit:${notification.id}`, `eventnotifications:delete:${notification.id}`]} anyPermissions>
          <DropdownButton id={`more-dropdown-${notification.id}`} title="More" pullRight>
            <IfPermitted permissions={`eventnotifications:edit:${notification.id}`}>
              <MenuItem disabled={isTestLoading} onClick={onTest(notification)}>
                {isTestLoading ? 'Testing...' : 'Test Notification'}
              </MenuItem>
            </IfPermitted>
            <MenuItem divider />
            <IfPermitted permissions={`eventnotifications:delete:${notification.id}`}>
              <DeleteMenuItem onClick={onDelete(notification)} />
            </IfPermitted>
          </DropdownButton>
        </IfPermitted>
      </ButtonToolbar>
    );
  }

  render() {
    const { notifications, pagination, query, onPageChange, onQueryChange } = this.props;
    const { notificationToShare } = this.state;

    const setNotificationToShare = (notification) => this.setState({ notificationToShare: notification });

    if (pagination.grandTotal === 0) {
      return renderEmptyContent();
    }

    return (
      <>
        <Row>
          <Col md={12}>
            <SearchForm query={query}
                        onSearch={onQueryChange}
                        onReset={onQueryChange}
                        placeholder="Find Notifications"
                        wrapperClass={styles.inline}
                        queryHelpComponent={<QueryHelper entityName="notification" />}
                        topMargin={0}
                        useLoadingState />

            <PaginatedList pageSizes={PAGE_SIZES}
                           totalItems={pagination.total}
                           onChange={onPageChange}>
              <div className={styles.notificationList}>
                <EntityList items={this.formatNotification(notifications, setNotificationToShare)} />
              </div>
            </PaginatedList>
          </Col>
        </Row>
        {notificationToShare && (
          <EntityShareModal entityId={notificationToShare.id}
                            entityType="notification"
                            description="Search for a User or Team to add as collaborator on this notification."
                            entityTitle={notificationToShare.title}
                            onClose={() => setNotificationToShare(undefined)} />
        )}
      </>
    );
  }
}

export default EventNotifications;
