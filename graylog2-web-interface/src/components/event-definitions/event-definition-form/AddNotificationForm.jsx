import React from 'react';
import PropTypes from 'prop-types';
import { Button, ButtonToolbar, Col, ControlLabel, FormGroup, HelpBlock, Row } from 'react-bootstrap';

import { Select } from 'components/common';

import commonStyles from '../common/commonStyles.css';

class AddNotificationForm extends React.Component {
  static propTypes = {
    notifications: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
  };

  state = {
    selectedNotification: undefined,
    displayNewNotificationForm: false,
  };

  handleSubmit = () => {
    const { onChange } = this.props;
    const { selectedNotification } = this.state;
    onChange(selectedNotification);
  };

  handleSelectNotificationChange = (nextNotificationId) => {
    if (nextNotificationId === 'create') {
      this.setState({ displayNewNotificationForm: true });
      return;
    }

    this.setState({ selectedNotification: nextNotificationId });
  };

  formatNotifications = (notifications) => {
    const formattedNotifications = notifications.map(n => ({ label: n.title, value: n.id }));
    formattedNotifications.unshift({ label: 'Create New Notification...', value: 'create' });
    return formattedNotifications;
  };

  render() {
    const { notifications, onCancel } = this.props;
    const { displayNewNotificationForm, selectedNotification } = this.state;

    return (
      <Row>
        <Col md={7} lg={6}>
          <h2 className={commonStyles.title}>Add Notification</h2>

          <fieldset>
            <FormGroup controlId="notification-select">
              <ControlLabel>Choose Notification</ControlLabel>
              <Select id="notification-select"
                      matchProp="label"
                      placeholder="Select Notification"
                      onChange={this.handleSelectNotificationChange}
                      options={this.formatNotifications(notifications)}
                      value={selectedNotification} />
              <HelpBlock>Select a Notification to use on this Event Definition or create a new Notification.</HelpBlock>
            </FormGroup>

            {displayNewNotificationForm && (
              <div>TBD</div>
            )}
          </fieldset>

          <ButtonToolbar>
            <Button bsStyle="primary" disabled={!selectedNotification} onClick={this.handleSubmit}>Done</Button>
            <Button onClick={onCancel}>Cancel</Button>
          </ButtonToolbar>
        </Col>
      </Row>
    );
  }
}

export default AddNotificationForm;
