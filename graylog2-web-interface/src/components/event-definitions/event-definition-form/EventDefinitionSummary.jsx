import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'react-bootstrap';
import lodash from 'lodash';
import { PluginStore } from 'graylog-web-plugin/plugin';

import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import { NOTIFICATION_TYPE } from 'components/event-notifications/event-notification-types';

import styles from './EventDefinitionSummary.css';
import commonStyles from '../common/commonStyles.css';

class EventDefinitionSummary extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    notifications: PropTypes.array.isRequired,
  };

  renderDetails = (eventDefinition) => {
    return (
      <React.Fragment>
        <h3 className={commonStyles.title}>Details</h3>
        <dl>
          <dt>Title</dt>
          <dd>{eventDefinition.title || 'No title given'}</dd>
          <dt>Description</dt>
          <dd>{eventDefinition.description || 'No description given'}</dd>
          <dt>Priority</dt>
          <dd>{lodash.upperFirst(EventDefinitionPriorityEnum.properties[eventDefinition.priority].name)}</dd>
        </dl>
      </React.Fragment>
    );
  };

  getPlugin = (name, type) => {
    if (type === undefined) {
      return {};
    }
    return PluginStore.exports(name).find(edt => edt.type === type);
  };

  renderCondition = (config) => {
    const conditionPlugin = this.getPlugin('eventDefinitionTypes', config.type);
    const component = (conditionPlugin.summaryComponent
      ? React.createElement(conditionPlugin.summaryComponent, { config: config })
      : <span>Condition plugin <em>{config.type}</em> does not provide a summary.</span>
    );

    return (
      <React.Fragment>
        <h3 className={commonStyles.title}>{conditionPlugin.displayName || config.type}</h3>
        {component}
      </React.Fragment>
    );
  };

  renderField = (fieldName, config, keys) => {
    if (!config.providers || config.providers.length === 0) {
      return <span key={fieldName}>No field value provider configured.</span>;
    }
    const provider = config.providers[0] || {};
    const fieldProviderPlugin = this.getPlugin('fieldValueProviders', provider.type);
    const component = (fieldProviderPlugin.summaryComponent
      ? React.createElement(fieldProviderPlugin.summaryComponent, {
        fieldName: fieldName,
        config: config,
        keys: keys,
      })
      : <span>Condition plugin <em>{provider.type}</em> does not provide a summary.</span>
    );

    return (
      <React.Fragment key={fieldName}>
        <h4 className={commonStyles.title}>{fieldName}</h4>
        {component}
      </React.Fragment>
    );
  };

  renderFields = (fields, keys) => {
    const fieldNames = Object.keys(fields);
    return (
      <React.Fragment>
        <h3 className={commonStyles.title}>Fields</h3>
        {fieldNames.length === 0
          ? <p>No Fields configured for Events based on this Definition.</p>
          : fieldNames.map(fieldName => this.renderField(fieldName, fields[fieldName], keys))
        }
      </React.Fragment>
    );
  };

  renderNotification = (notificationAction) => {
    const { notifications } = this.props;
    const notification = notifications.find(n => n.id === notificationAction.notification_id);
    const notificationPlugin = this.getPlugin('eventNotificationTypes', notification.config.type);
    const component = (notificationPlugin.summaryComponent
      ? React.createElement(notificationPlugin.summaryComponent, {
        type: notificationPlugin.displayName,
        notification: notification,
        action: notificationAction,
      })
      : <span>Notification plugin <em>{notification.config.type}</em> does not provide a summary.</span>
    );

    return (
      <React.Fragment key={notificationAction.notification_id}>
        {component}
      </React.Fragment>
    );
  };

  renderNotifications = (actions) => {
    const notificationActions = actions.filter(action => action.type === NOTIFICATION_TYPE);

    return (
      <React.Fragment>
        <h3 className={commonStyles.title}>Notifications</h3>
        {notificationActions.length === 0
          ? <p>This Event is not configured to trigger any Notifications.</p>
          : notificationActions.map(this.renderNotification)}
      </React.Fragment>
    );
  };

  render() {
    const { eventDefinition } = this.props;
    return (
      <Row>
        <Col md={12}>
          <h2 className={commonStyles.title}>Event Summary</h2>
          <Row className={styles.eventSummary}>
            <Col md={3}>
              {this.renderDetails(eventDefinition)}
            </Col>
            <Col md={3}>
              {this.renderCondition(eventDefinition.config)}
            </Col>
            <Col md={3}>
              {this.renderFields(eventDefinition.field_spec, eventDefinition.key_spec)}
            </Col>
            <Col md={3}>
              {this.renderNotifications(eventDefinition.actions)}
            </Col>
          </Row>
        </Col>
      </Row>
    );
  }
}

export default EventDefinitionSummary;
