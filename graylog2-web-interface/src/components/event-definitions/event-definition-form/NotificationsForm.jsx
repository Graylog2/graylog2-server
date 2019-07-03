import React from 'react';
import PropTypes from 'prop-types';
import { Button, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import lodash from 'lodash';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { DataTable } from 'components/common';
import Routes from 'routing/Routes';

import AddNotificationForm from './AddNotificationForm';

import styles from './NotificationsForm.css';
import commonStyles from '../common/commonStyles.css';

const NOTIFICATION_TYPE = 'trigger-notification-v1';

class NotificationsForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    notifications: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  state = {
    showAddNotification: false,
  };

  toggleAddNotification = () => {
    const { showAddNotification } = this.state;
    this.setState({ showAddNotification: !showAddNotification });
  };

  handleAddNotification = (nextNotification) => {
    const { onChange, eventDefinition } = this.props;
    const nextActions = lodash.cloneDeep(eventDefinition.actions);
    nextActions.push({
      type: NOTIFICATION_TYPE,
      notification_id: nextNotification,
    });
    onChange('actions', nextActions);
    this.toggleAddNotification();
  };

  getNotificationPlugin = (type) => {
    if (type === undefined) {
      return {};
    }
    return PluginStore.exports('eventNotificationTypes').find(n => n.type === type);
  };

  notificationFormatter = (notification) => {
    const plugin = this.getNotificationPlugin(notification.config.type);

    return (
      <tr key={notification.id}>
        <td>{notification.title}</td>
        <td>{plugin.displayName || notification.config.type}</td>
        <td className="actions"><Button bsStyle="info" bsSize="xsmall">Remove from Event</Button></td>
      </tr>
    );
  };

  render() {
    const { eventDefinition, notifications } = this.props;
    const { showAddNotification } = this.state;

    if (showAddNotification) {
      return (
        <AddNotificationForm notifications={notifications}
                             onChange={this.handleAddNotification}
                             onCancel={this.toggleAddNotification} />
      );
    }

    let content;
    const notificationActions = eventDefinition.actions
      .filter(action => action.type === NOTIFICATION_TYPE)
      .map(action => notifications.find(n => n.id === action.notification_id));

    if (notificationActions.length === 0) {
      content = (
        <p>
          This Event is not configured to trigger any Notifications yet.{' '}
          <Button className="btn-text" bsStyle="link" bsSize="small" onClick={this.toggleAddNotification}>
            Get notified
          </Button>.
        </p>
      );
    } else {
      content = (
        <Row>
          <Col md={8} lg={6}>
            <DataTable id="event-definition-notifications"
                       className="table-striped table-hover"
                       headers={['Notification', 'Type', 'Actions']}
                       sortByKey="title"
                       rows={notificationActions}
                       dataRowFormatter={this.notificationFormatter}
                       filterKeys={[]} />
            <Button bsStyle="success" onClick={this.toggleAddNotification}>
              Add Notification
            </Button>
          </Col>
        </Row>
      );
    }

    return (
      <Row>
        <Col md={12}>
          <span className={styles.manageNotifications}>
            <LinkContainer to={Routes.NEXT_ALERTS.NOTIFICATIONS.LIST} target="_blank">
              <Button bsStyle="link" bsSize="small">Manage Notifications <i className="fa fa-external-link" /></Button>
            </LinkContainer>
          </span>
          <h2 className={commonStyles.title}>Notifications</h2>
          {content}
        </Col>
      </Row>
    );
  }
}

export default NotificationsForm;
