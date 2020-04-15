import React from 'react';
import PropTypes from 'prop-types';
import { Well } from 'components/graylog';

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
        <>
          <tr>
            <td>Sender</td>
            <td>{notification.config.sender}</td>
          </tr>
          <tr>
            <td>Subject</td>
            <td>{notification.config.subject}</td>
          </tr>
          <tr>
            <td>User recipients</td>
            <td>{notification.config.user_recipients.join(', ') || 'No users will receive this notification.'}</td>
          </tr>
          <tr>
            <td>Email recipients</td>
            <td>
              {notification.config.email_recipients.join(', ') || 'No email addresses are configured to receive this notification.'}
            </td>
          </tr>
          <tr>
            <td>Email Body</td>
            <td>
              <Well bsSize="small" className={styles.bodyPreview}>
                {notification.config.body_template || <em>Empty body</em>}
              </Well>
            </td>
          </tr>
        </>
      </CommonNotificationSummary>
    );
  }
}

export default EmailNotificationSummary;
