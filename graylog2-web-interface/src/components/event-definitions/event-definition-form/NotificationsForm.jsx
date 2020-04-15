import React from 'react';
import PropTypes from 'prop-types';
import { LinkContainer } from 'react-router-bootstrap';
import lodash from 'lodash';

import { Col, Row, Button } from 'components/graylog';
import { Icon } from 'components/common';
import Routes from 'routing/Routes';
import PermissionsMixin from 'util/PermissionsMixin';

import AddNotificationForm from './AddNotificationForm';
import NotificationSettingsForm from './NotificationSettingsForm';
import NotificationList from './NotificationList';

import styles from './NotificationsForm.css';
import commonStyles from '../common/commonStyles.css';

class NotificationsForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    notifications: PropTypes.array.isRequired,
    defaults: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  state = {
    showAddNotificationForm: false,
  };

  toggleAddNotificationForm = () => {
    const { showAddNotificationForm } = this.state;
    this.setState({ showAddNotificationForm: !showAddNotificationForm });
  };

  handleAssignNotification = (nextNotification) => {
    const { onChange, eventDefinition } = this.props;
    const nextNotifications = lodash.cloneDeep(eventDefinition.notifications);
    nextNotifications.push({
      notification_id: nextNotification,
    });
    onChange('notifications', nextNotifications);
    this.toggleAddNotificationForm();
  };

  handleRemoveNotification = (notificationId) => {
    const { onChange, eventDefinition } = this.props;
    const notification = eventDefinition.notifications.find((n) => n.notification_id === notificationId);
    const nextNotifications = lodash.without(eventDefinition.notifications, notification);
    onChange('notifications', nextNotifications);
  };

  render() {
    const { eventDefinition, notifications, defaults, currentUser, onChange } = this.props;
    const { showAddNotificationForm } = this.state;

    if (!PermissionsMixin.isPermitted(currentUser.permissions, 'eventnotifications:read')) {
      return (
        <Row>
          <Col md={6} lg={5}>
            <p>No Notifications found.</p>
          </Col>
        </Row>
      );
    }

    if (showAddNotificationForm) {
      return (
        <AddNotificationForm notifications={notifications}
                             onChange={this.handleAssignNotification}
                             onCancel={this.toggleAddNotificationForm}
                             hasCreationPermissions={
                               PermissionsMixin.isPermitted(currentUser.permissions, 'eventnotifications:create')
                             } />
      );
    }

    return (
      <Row>
        <Col md={6} lg={5}>
          <span className={styles.manageNotifications}>
            <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.LIST} target="_blank">
              <Button bsStyle="link" bsSize="small">Manage Notifications <Icon name="external-link" /></Button>
            </LinkContainer>
          </span>
          <h2 className={commonStyles.title}>Notifications <small>(optional)</small></h2>
          <p>
            Is this Event important enough that requires your attention? Make it an Alert by adding Notifications to it.
          </p>

          <NotificationList eventDefinition={eventDefinition}
                            notifications={notifications}
                            onAddNotificationClick={this.toggleAddNotificationForm}
                            onRemoveNotificationClick={this.handleRemoveNotification} />
        </Col>
        <Col md={4} lg={3} mdOffset={1} className={styles.notificationSettings}>
          <NotificationSettingsForm eventDefinition={eventDefinition}
                                    defaults={defaults}
                                    onSettingsChange={onChange} />
        </Col>
      </Row>
    );
  }
}

export default NotificationsForm;
