import React from 'react';
import PropTypes from 'prop-types';
import { Button, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { EmptyEntity } from 'components/common';

import Routes from 'routing/Routes';

class EventNotifications extends React.Component {
  static propTypes = {
    notifications: PropTypes.object.isRequired,
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
            <LinkContainer to={Routes.NEXT_ALERTS.NOTIFICATIONS.CREATE}>
              <Button bsStyle="success">Get Started!</Button>
            </LinkContainer>
          </EmptyEntity>
        </Col>
      </Row>
    );
  };


  render() {
    return this.renderEmptyContent();
  }
}

export default EventNotifications;
