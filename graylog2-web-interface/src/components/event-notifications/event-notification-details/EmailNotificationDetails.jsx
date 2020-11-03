import * as React from 'react';
import PropTypes from 'prop-types';

import { ReadOnlyFormGroup } from 'components/common';
import { Well } from 'components/graylog';

import styles from '../event-notification-types/EmailNotificationSummary.css';

const EmailNotificationDetails = ({ notification }) => (
  <>
    <ReadOnlyFormGroup label="Sender" value={notification.config.sender} />
    <ReadOnlyFormGroup label="Subject" value={notification.config.subject} />
    <ReadOnlyFormGroup label="User recipients" value={notification.config.user_recipients.join(', ') || 'No users will receive this notification.'} />
    <ReadOnlyFormGroup label="Email recipients" value={notification.config.email_recipients.join(', ') || 'No email addresses are configured to receive this notification.'} />
    <ReadOnlyFormGroup label="Email Body"
                       value={(
                         <Well bsSize="small" className={styles.bodyPreview}>
                           {notification.config.body_template || <em>Empty body</em>}
                         </Well>
                       )} />
  </>
);

EmailNotificationDetails.propTypes = {
  notification: PropTypes.object.isRequired,
};

export default EmailNotificationDetails;
