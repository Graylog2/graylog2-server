import * as React from 'react';
import PropTypes from 'prop-types';

import { LinkContainer } from 'components/graylog/router';
import { IfPermitted } from 'components/common';
import { Button, ButtonToolbar } from 'components/graylog';
import Routes from 'routing/Routes';

const EventNotificationActionLinks = ({ notificationId }) => (
  <ButtonToolbar>
    <IfPermitted permissions={`eventnotifications:read:${notificationId}`}>
      <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.show(notificationId)}>
        <Button bsStyle="success">View Details</Button>
      </LinkContainer>
    </IfPermitted>
    <IfPermitted permissions={`eventnotifications:edit:${notificationId}`}>
      <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.edit(notificationId)}>
        <Button bsStyle="success">Edit Notification</Button>
      </LinkContainer>
    </IfPermitted>
  </ButtonToolbar>
);

EventNotificationActionLinks.propTypes = {
  notificationId: PropTypes.string.isRequired,
};

export default EventNotificationActionLinks;
