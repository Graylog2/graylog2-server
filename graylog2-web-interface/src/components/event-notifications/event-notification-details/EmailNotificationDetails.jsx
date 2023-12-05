/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import PropTypes from 'prop-types';

import { ReadOnlyFormGroup } from 'components/common';
import { Well } from 'components/bootstrap';

import styles from '../event-notification-types/EmailNotificationSummary.css';

const EmailNotificationDetails = ({ notification }) => {
  const recipients = (
    <ReadOnlyFormGroup label="Email Recipients"
                       value={notification.config.email_recipients.join(', ') || 'No email addresses are configured to receive this notification.'} />
  );
  const recipientLookupInfo = (
    <>
      <ReadOnlyFormGroup label="Email Recipients Lookup Table Name" value={notification.config.recipients_lut_name} />
      <ReadOnlyFormGroup label="Email Recipients Lookup Table Key" value={notification.config.recipients_lut_key} />
    </>
  );
  const sender = (
    <ReadOnlyFormGroup label="Sender" value={notification.config.sender} />
  );
  const senderLookupInfo = (
    <>
      <ReadOnlyFormGroup label="Sender Lookup Table Name" value={notification.config.sender_lut_name} />
      <ReadOnlyFormGroup label="Sender Lookup Table Key" value={notification.config.sender_lut_key} />
    </>
  );
  const replyTo = (
    <ReadOnlyFormGroup label="Reply-To" value={notification.config.reply_to} />
  );
  const replyToLookupInfo = (
    <>
      <ReadOnlyFormGroup label="Reply-To Lookup Table Name" value={notification.config.reply_to_lut_name} />
      <ReadOnlyFormGroup label="Reply-To Lookup Table Key" value={notification.config.reply_to_lut_key} />
    </>
  );

  return (
    <>
      <ReadOnlyFormGroup label="Use Lookup Table for Sender" value={notification.config.lookup_sender_email ? 'Yes' : 'No'} />
      {notification.config.lookup_sender_email ? senderLookupInfo : sender}
      <ReadOnlyFormGroup label="Subject" value={notification.config.subject} />
      <ReadOnlyFormGroup label="Use Lookup Table for Reply-To" value={notification.config.lookup_reply_to_email ? 'Yes' : 'No'} />
      {notification.config.lookup_reply_to_email ? replyToLookupInfo : replyTo}
      <ReadOnlyFormGroup label="User Recipients" value={notification.config.user_recipients.join(', ') || 'No users will receive this notification.'} />
      <ReadOnlyFormGroup label="Use Lookup Table for Email Recipients" value={notification.config.lookup_recipient_emails ? 'Yes' : 'No'} />
      {notification.config.lookup_recipient_emails ? recipientLookupInfo : recipients}
      <ReadOnlyFormGroup label="Time Zone" value={notification.config.time_zone} />
      <ReadOnlyFormGroup label="Email Body"
                         value={(
                           <Well bsSize="small" className={styles.bodyPreview}>
                             {notification.config.body_template || <em>Empty body</em>}
                           </Well>
                       )} />
      <ReadOnlyFormGroup label="Email HTML Body"
                         value={(
                           <Well bsSize="small" className={styles.bodyPreview}>
                             {notification.config.html_body_template || <em>Empty body</em>}
                           </Well>
                       )} />
    </>
  );
};

EmailNotificationDetails.propTypes = {
  notification: PropTypes.object.isRequired,
};

export default EmailNotificationDetails;
