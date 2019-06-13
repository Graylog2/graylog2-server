import React from 'react';
import PropTypes from 'prop-types';
import { Button, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';

import styles from './NotificationsForm.css';
import commonStyles from '../common/commonStyles.css';

class NotificationsForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  render() {
    // TODO: Ensure route to notifications is right after changes
    return (
      <Row>
        <Col md={12}>
          <span className={styles.manageNotifications}>
            <LinkContainer to={Routes.ALERTS.NOTIFICATIONS} target="_blank">
              <Button bsStyle="link" bsSize="small">Manage Notifications <i className="fa fa-external-link" /></Button>
            </LinkContainer>
          </span>
          <h2 className={commonStyles.title}>Notifications</h2>
          <p>
            This Event is not configured to trigger any Notifications yet.{' '}
            <Button className="btn-text" bsStyle="link" bsSize="small">Get notified</Button>.
          </p>
        </Col>
      </Row>
    );
  }
}

export default NotificationsForm;
