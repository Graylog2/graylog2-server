import React from 'react';
import PropTypes from 'prop-types';
import { Button, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import lodash from 'lodash';

import Routes from 'routing/Routes';
import { NOTIFICATION_TYPE } from 'components/event-notifications/event-notification-types';

import AddNotificationForm from './AddNotificationForm';
import NotificationList from './NotificationList';

import styles from './NotificationsForm.css';
import commonStyles from '../common/commonStyles.css';

class NotificationsForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    notifications: PropTypes.array.isRequired,
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
    const nextActions = lodash.cloneDeep(eventDefinition.actions);
    nextActions.push({
      type: NOTIFICATION_TYPE,
      notification_id: nextNotification,
    });
    onChange('actions', nextActions);
    this.toggleAddNotificationForm();
  };

  handleRemoveNotification = (notificationId) => {

  };

  render() {
    const { eventDefinition, notifications } = this.props;
    const { showAddNotificationForm } = this.state;

    if (showAddNotificationForm) {
      return (
        <AddNotificationForm notifications={notifications}
                             onChange={this.handleAssignNotification}
                             onCancel={this.toggleAddNotificationForm} />
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
          <NotificationList eventDefinition={eventDefinition}
                            notifications={notifications}
                            onAddNotificationClick={this.toggleAddNotificationForm}
                            onRemoveNotificationClick={this.handleRemoveNotification} />
        </Col>
      </Row>
    );
  }
}

export default NotificationsForm;
