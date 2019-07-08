import React from 'react';
import PropTypes from 'prop-types';

import CommonNotificationSummary from './CommonNotificationSummary';

class EmailNotificationSummary extends React.Component {
  static propTypes = {
    type: PropTypes.string.isRequired,
    notification: PropTypes.object,
    definitionNotification: PropTypes.object.isRequired,
  };

  static defaultProps = {
    notification: {},
  };

  render() {
    const { notification } = this.props;
    return (
      <CommonNotificationSummary {...this.props}>
        <React.Fragment>
          <dt>User recipients</dt>
          <dd>{notification.config.user_recipients.join(', ') || 'No users will receive this notification.'}</dd>
          <dt>Email recipients</dt>
          <dd>{notification.config.email_recipients.join(', ') || 'No email addresses are configured to receive this notification.'}</dd>
        </React.Fragment>
      </CommonNotificationSummary>
    );
  }
}

export default EmailNotificationSummary;
