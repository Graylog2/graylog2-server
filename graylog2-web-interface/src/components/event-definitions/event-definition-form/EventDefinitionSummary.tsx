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
import * as React from 'react';
import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import upperFirst from 'lodash/upperFirst';
import { PluginStore } from 'graylog-web-plugin/plugin';
import moment from 'moment';

import 'moment-duration-format';
import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { MarkdownPreview } from 'components/common/MarkdownEditor';
import { Alert, Col, Row } from 'components/bootstrap';
import { isPermitted } from 'util/PermissionsMixin';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import type User from 'logic/users/User';

import EventDefinitionValidationSummary from './EventDefinitionValidationSummary';
import styles from './EventDefinitionSummary.css';

import type { EventDefinition } from '../event-definitions-types';
import commonStyles from '../common/commonStyles.css';
import { SYSTEM_EVENT_DEFINITION_TYPE } from '../constants';

type Props = {
  eventDefinition: Omit<EventDefinition, 'id'>,
  notifications: Array<any>,
  validation: {
    errors: {
      title?: string,
    }
  },
  currentUser: User,
}

const EventDefinitionSummary = ({ eventDefinition, notifications, validation, currentUser }: Props) => {
  const [showValidation, setShowValidation] = useState<boolean>(false);

  useEffect(() => {
    const flipShowValidation = () => {
      if (!showValidation) {
        setShowValidation(true);
      }
    };

    flipShowValidation();
  }, [showValidation, setShowValidation]);

  const renderDetails = () => (
    <>
      <h3 className={commonStyles.title}>Details</h3>
      <dl>
        <dt>Title</dt>
        <dd>{eventDefinition.title || 'No title given'}</dd>
        <dt>Description</dt>
        <dd>{eventDefinition.description || 'No description given'}</dd>
        <dt>Priority</dt>
        <dd>{upperFirst(EventDefinitionPriorityEnum.properties[eventDefinition.priority].name)}</dd>
        <dt style={{ margin: '16px 0 0' }}>Remediation Steps</dt>
        <dd>
          <MarkdownPreview show
                           withFullView
                           noBorder
                           noBackground
                           value={eventDefinition.remediation_steps || 'No remediation steps given'} />
        </dd>
      </dl>
    </>
  );

  const getPlugin = (name, type) => {
    if (type === undefined) {
      return {};
    }

    return PluginStore.exports(name).find((edt) => edt.type === type) || {};
  };

  const renderCondition = (config) => {
    const conditionPlugin = getPlugin('eventDefinitionTypes', config.type);
    const component = (conditionPlugin.summaryComponent
      ? React.createElement(conditionPlugin.summaryComponent, {
        config,
        currentUser,
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

  const renderField = (fieldName, config, keys) => {
    if (!config.providers || config.providers.length === 0) {
      return <span key={fieldName}>No field value provider configured.</span>;
    }

    const provider = config.providers[0] || {};
    const fieldProviderPlugin = getPlugin('fieldValueProviders', provider.type);

    return (fieldProviderPlugin.summaryComponent
      ? React.createElement(fieldProviderPlugin.summaryComponent, {
        fieldName,
        config,
        keys: keys,
        key: fieldName,
        currentUser,
      })
      : <p key={fieldName}>Provider plugin <em>{provider.type}</em> does not provide a summary.</p>
    );
  };

  const renderFieldList = (fieldNames, fields, keys) => (
    <>
      <dl>
        <dt>Keys</dt>
        <dd>{keys.length > 0 ? keys.join(', ') : 'No Keys configured for Events based on this Definition.'}</dd>
      </dl>
      {fieldNames.sort(naturalSort).map((fieldName) => renderField(fieldName, fields[fieldName], keys))}
    </>
  );

  const renderFields = (fields, keys) => {
    const fieldNames = Object.keys(fields);

    return (
      <>
        <h3 className={commonStyles.title}>Fields</h3>
        {fieldNames.length === 0
          ? <p>No Fields configured for Events based on this Definition.</p>
          : renderFieldList(fieldNames, fields, keys)}
      </>
    );
  };

  const renderNotification = (definitionNotification) => {
    const notification = notifications.find((n) => n.id === definitionNotification.notification_id);

    let content;

    if (notification) {
      const notificationPlugin = getPlugin('eventNotificationTypes', notification.config.type);

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

  const renderNotificationSettings = (notificationSettings) => {
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

  const renderNotifications = (definitionNotifications, notificationSettings) => {
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
              {renderNotificationSettings(notificationSettings)}
              {definitionNotifications.map(renderNotification)}
            </>
          )}
      </>
    );
  };

  const isSystemEventDefinition = eventDefinition.config.type === SYSTEM_EVENT_DEFINITION_TYPE;

  return (
    <Row className={styles.eventSummary}>
      <Col md={12}>
        <h2 className={commonStyles.title}>Event Summary</h2>
        {showValidation && <EventDefinitionValidationSummary validation={validation} />}
        <Row>
          <Col md={5}>
            {renderDetails()}
          </Col>

          {!isSystemEventDefinition && (
            <Col md={5} mdOffset={1}>
              {renderCondition(eventDefinition.config)}
            </Col>
          )}
        </Row>
        <Row>
          {!isSystemEventDefinition && (
            <Col md={5}>
              {renderFields(eventDefinition.field_spec, eventDefinition.key_spec)}
            </Col>
          )}
          <Col md={5} mdOffset={isSystemEventDefinition ? 0 : 1}>
            {renderNotifications(eventDefinition.notifications, eventDefinition.notification_settings)}
          </Col>
        </Row>
      </Col>
    </Row>
  );
};

EventDefinitionSummary.propTypes = {
  eventDefinition: PropTypes.object.isRequired,
  notifications: PropTypes.array.isRequired,
  validation: PropTypes.object,
  currentUser: PropTypes.object.isRequired,
};

EventDefinitionSummary.defaultProps = {
  validation: undefined,
};

export default EventDefinitionSummary;
