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
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { PluginStore } from 'graylog-web-plugin/plugin';
import moment from 'moment';
import {} from 'moment-duration-format';
import naturalSort from 'javascript-natural-sort';

import { Alert, Col, Row } from 'components/graylog';
import { isPermitted } from 'util/PermissionsMixin';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';

// Import built-in plugins
import {} from 'components/event-definitions/event-definition-types';
import {} from 'components/event-notifications/event-notification-types';

import EventDefinitionValidationSummary from './EventDefinitionValidationSummary';
import styles from './EventDefinitionSummary.css';

import commonStyles from '../common/commonStyles.css';

class EventDefinitionSummary extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    notifications: PropTypes.array.isRequired,
    validation: PropTypes.object,
    currentUser: PropTypes.object.isRequired,
  };

  static defaultProps = {
    validation: undefined,
  };

  constructor(props) {
    super(props);

    this.state = {
      showValidation: false,
    };
  }

  componentDidUpdate() {
    this.showValidation();
  }

  showValidation = () => {
    const { showValidation } = this.state;

    if (!showValidation) {
      this.setState({ showValidation: true });
    }
  };

  renderDetails = (eventDefinition) => {
    return (
      <>
        <h3 className={commonStyles.title}>Details</h3>
        <dl>
          <dt>Title</dt>
          <dd>{eventDefinition.title || 'No title given'}</dd>
          <dt>Description</dt>
          <dd>{eventDefinition.description || 'No description given'}</dd>
          <dt>Priority</dt>
          <dd>{lodash.upperFirst(EventDefinitionPriorityEnum.properties[eventDefinition.priority].name)}</dd>
        </dl>
      </>
    );
  };

  getPlugin = (name, type) => {
    if (type === undefined) {
      return {};
    }

    return PluginStore.exports(name).find((edt) => edt.type === type) || {};
  };

  renderCondition = (config) => {
    const { currentUser } = this.props;
    const conditionPlugin = this.getPlugin('eventDefinitionTypes', config.type);
    const component = (conditionPlugin.summaryComponent
      ? React.createElement(conditionPlugin.summaryComponent, {
        config: config,
        currentUser: currentUser,
      })
      : <p>Condition plugin <em>{config.type}</em> does not provide a summary.</p>
    );

    return (
      <>
        <h3 className={commonStyles.title}>{conditionPlugin.displayName || config.type}</h3>
        {component}
      </>
    );
  };

  renderField = (fieldName, config, keys) => {
    const { currentUser } = this.props;

    if (!config.providers || config.providers.length === 0) {
      return <span key={fieldName}>No field value provider configured.</span>;
    }

    const provider = config.providers[0] || {};
    const fieldProviderPlugin = this.getPlugin('fieldValueProviders', provider.type);

    return (fieldProviderPlugin.summaryComponent
      ? React.createElement(fieldProviderPlugin.summaryComponent, {
        fieldName: fieldName,
        config: config,
        keys: keys,
        key: fieldName,
        currentUser: currentUser,
      })
      : <p key={fieldName}>Provider plugin <em>{provider.type}</em> does not provide a summary.</p>
    );
  };

  renderFieldList = (fieldNames, fields, keys) => {
    return (
      <>
        <dl>
          <dt>Keys</dt>
          <dd>{keys.length > 0 ? keys.join(', ') : 'No Keys configured for Events based on this Definition.'}</dd>
        </dl>
        {fieldNames.sort(naturalSort).map((fieldName) => this.renderField(fieldName, fields[fieldName], keys))}
      </>
    );
  };

  renderFields = (fields, keys) => {
    const fieldNames = Object.keys(fields);

    return (
      <>
        <h3 className={commonStyles.title}>Fields</h3>
        {fieldNames.length === 0
          ? <p>No Fields configured for Events based on this Definition.</p>
          : this.renderFieldList(fieldNames, fields, keys)}
      </>
    );
  };

  renderNotification = (definitionNotification) => {
    const { notifications } = this.props;
    const notification = notifications.find((n) => n.id === definitionNotification.notification_id);

    let content;

    if (notification) {
      const notificationPlugin = this.getPlugin('eventNotificationTypes', notification.config.type);

      content = (notificationPlugin.summaryComponent
        ? React.createElement(notificationPlugin.summaryComponent, {
          type: notificationPlugin.displayName,
          notification: notification,
          definitionNotification: definitionNotification,
        })
        : <p>Notification plugin <em>{notification.config.type}</em> does not provide a summary.</p>
      );
    } else {
      content = (
        <p>
          Could not find information for Notification <em>{definitionNotification.notification_id}</em>.
        </p>
      );
    }

    return (
      <React.Fragment key={definitionNotification.notification_id}>
        {content}
      </React.Fragment>
    );
  };

  renderNotificationSettings = (notificationSettings) => {
    const formattedDuration = moment.duration(notificationSettings.grace_period_ms)
      .format('d [days] h [hours] m [minutes] s [seconds]', { trim: 'all' });

    const formattedGracePeriod = (notificationSettings.grace_period_ms
      ? `Grace Period is set to ${formattedDuration}`
      : 'Grace Period is disabled');

    const formattedBacklogSize = (notificationSettings.backlog_size
      ? `Notifications will include ${notificationSettings.backlog_size} messages`
      : 'Notifications will not include any messages.');

    return (
      <>
        <h4>Settings</h4>
        <dl>
          <dd>{formattedGracePeriod}</dd>
          <dd>{formattedBacklogSize}</dd>
        </dl>
      </>
    );
  };

  renderNotifications = (definitionNotifications, notificationSettings) => {
    const { currentUser } = this.props;

    const effectiveDefinitionNotifications = definitionNotifications
      .filter((n) => isPermitted(currentUser.permissions, `eventnotifications:read:${n.notification_id}`));
    const notificationsWithMissingPermissions = definitionNotifications
      .filter((n) => !effectiveDefinitionNotifications.map((nObj) => nObj.notification_id).includes(n.notification_id));
    const warning = notificationsWithMissingPermissions.length > 0
      ? (
        <Alert bsStyle="warning">
          Missing Notifications Permissions for:<br />
          {notificationsWithMissingPermissions.map((n) => n.notification_id).join(', ')}
        </Alert>
      )
      : null;

    return (
      <>
        <h3 className={commonStyles.title}>Notifications</h3>
        <p>
          {warning}
        </p>
        {effectiveDefinitionNotifications.length === 0 && notificationsWithMissingPermissions.length <= 0
          ? <p>This Event is not configured to trigger any Notifications.</p>
          : (
            <>
              {this.renderNotificationSettings(notificationSettings)}
              {definitionNotifications.map(this.renderNotification)}
            </>
          )}
      </>
    );
  };

  render() {
    const { eventDefinition, validation } = this.props;
    const { showValidation } = this.state;

    return (
      <Row className={styles.eventSummary}>
        <Col md={12}>
          <h2 className={commonStyles.title}>Event Summary</h2>
          {showValidation && <EventDefinitionValidationSummary validation={validation} />}
          <Row>
            <Col md={5}>
              {this.renderDetails(eventDefinition)}
            </Col>
            <Col md={5} mdOffset={1}>
              {this.renderCondition(eventDefinition.config)}
            </Col>
          </Row>
          <Row>
            <Col md={5}>
              {this.renderFields(eventDefinition.field_spec, eventDefinition.key_spec)}
            </Col>
            <Col md={5} mdOffset={1}>
              {this.renderNotifications(eventDefinition.notifications, eventDefinition.notification_settings)}
            </Col>
          </Row>
        </Col>
      </Row>
    );
  }
}

export default EventDefinitionSummary;
