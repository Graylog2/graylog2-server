import React from 'react';
import PropTypes from 'prop-types';

import CommonNotificationSummary from './CommonNotificationSummary';

import styles from './EmailNotificationSummary.css';

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
          <dt>Sender</dt>
          <dd>{notification.config.sender}</dd>
          <dt>Subject</dt>
          <dd>{notification.config.subject}</dd>
          <dt>User recipients</dt>
          <dd>{notification.config.user_recipients.join(', ') || 'No users will receive this notification.'}</dd>
          <dt>Email recipients</dt>
          <dd>{notification.config.email_recipients.join(', ') || 'No email addresses are configured to receive this notification.'}</dd>
          <dt>Email Body</dt>
          <dd><pre className={`${styles.bodyPreview} pre-scrollable`}>{notification.config.body_template}</pre></dd>
        </React.Fragment>
      </CommonNotificationSummary>
    );
  }
}

export default EmailNotificationSummary;
