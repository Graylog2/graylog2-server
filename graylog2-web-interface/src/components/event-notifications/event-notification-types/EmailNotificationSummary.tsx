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
import React from 'react';

import { Well } from 'components/bootstrap';

import CommonNotificationSummary from './CommonNotificationSummary';
import styles from './EmailNotificationSummary.css';

type EmailNotificationSummaryProps = {
  type: string;
  notification?: any;
  definitionNotification: any;
};

const EmailNotificationSummary = ({
  notification = {},
  ...otherProps
}: EmailNotificationSummaryProps) => (
  <CommonNotificationSummary notification={notification} {...otherProps}>
    <>
      <tr>
        <td>Use Lookup Table for Sender</td>
        <td>{notification.config.lookup_sender_email ? 'Yes' : 'No'}</td>
      </tr>
      {notification.config.lookup_sender_email ? (
        <>
          <tr>
            <td>Sender Email Lookup Table Name</td>
            <td>{notification.config.sender_lut_name}</td>
          </tr>
          <tr>
            <td>Sender Email Lookup Table Key</td>
            <td>{notification.config.sender_lut_key}</td>
          </tr>
        </>
      )
        : (
          <tr>
            <td>Sender</td>
            <td>{notification.config.sender}</td>
          </tr>
        )}
      <tr>
        <td>Subject</td>
        <td>{notification.config.subject}</td>
      </tr>
      <tr>
        <td>Send as Single Email</td>
        <td>{notification.config.single_email}</td>
      </tr>
      <tr>
        <td>Use Lookup Table for Reply-To</td>
        <td>{notification.config.lookup_reply_to_email ? 'Yes' : 'No'}</td>
      </tr>
      {notification.config.lookup_reply_to_email ? (
        <>
          <tr>
            <td>Reply-To Email Lookup Table Name</td>
            <td>{notification.config.reply_to_lut_name}</td>
          </tr>
          <tr>
            <td>Reply-To Email Lookup Table Key</td>
            <td>{notification.config.reply_to_lut_key}</td>
          </tr>
        </>
      )
        : (
          <tr>
            <td>Reply-To</td>
            <td>{notification.config.reply_to}</td>
          </tr>
        )}

      <tr>
        <td>User Recipients</td>
        <td>{notification.config.user_recipients.join(', ') || 'No users will receive this notification.'}</td>
      </tr>
      <tr>
        <td>Use Lookup Table for Email Recipients</td>
        <td>{notification.config.lookup_recipient_emails ? 'Yes' : 'No'}</td>
      </tr>
      {notification.config.lookup_recipient_emails ? (
        <>
          <tr>
            <td>Email Recipients Lookup Table Name</td>
            <td>{notification.config.recipients_lut_name}</td>
          </tr>
          <tr>
            <td>Email Recipients Lookup Table Key</td>
            <td>{notification.config.recipients_lut_key}</td>
          </tr>
        </>
      )
        : (
          <tr>
            <td>Email Recipients</td>
            <td>
              {notification.config.email_recipients.join(', ') || 'No email addresses are configured to receive this notification.'}
            </td>
          </tr>
        )}

      <tr>
        <td>Users to CC</td>
        <td>{notification.config.cc_users.join(', ') || 'No users will be cc\'d on this notification.'}</td>
      </tr>
      <tr>
        <td>Use Lookup Table for CC Emails</td>
        <td>{notification.config.lookup_cc_emails ? 'Yes' : 'No'}</td>
      </tr>
      {notification.config.lookup_cc_emails ? (
        <>
          <tr>
            <td>CC Emails Lookup Table Name</td>
            <td>{notification.config.cc_emails_lut_name}</td>
          </tr>
          <tr>
            <td>CC Emails Lookup Table Key</td>
            <td>{notification.config.cc_emails_lut_key}</td>
          </tr>
        </>
      )
        : (
          <tr>
            <td>CC Emails</td>
            <td>
              {notification.config.cc_emails.join(', ') || 'No email addresses are configured to be cc\'d on this notification.'}
            </td>
          </tr>
        )}

      <tr>
        <td>Users to BCC</td>
        <td>{notification.config.bcc_users.join(', ') || 'No users will be bcc\'d on this notification.'}</td>
      </tr>
      <tr>
        <td>Use Lookup Table for BCC Emails</td>
        <td>{notification.config.lookup_bcc_emails ? 'Yes' : 'No'}</td>
      </tr>
      {notification.config.lookup_bcc_emails ? (
        <>
          <tr>
            <td>BCC Emails Lookup Table Name</td>
            <td>{notification.config.bcc_emails_lut_name}</td>
          </tr>
          <tr>
            <td>BCC Emails Lookup Table Key</td>
            <td>{notification.config.bcc_emails_lut_key}</td>
          </tr>
        </>
      )
        : (
          <tr>
            <td>BCC Emails</td>
            <td>
              {notification.config.bcc_emails.join(', ') || 'No email addresses are configured to be bcc\'d on this notification.'}
            </td>
          </tr>
        )}

      <tr>
        <td>Email Body</td>
        <td>
          <Well bsSize="small" className={styles.bodyPreview}>
            {notification.config.body_template || <em>Empty body</em>}
          </Well>
        </td>
      </tr>
      <tr>
        <td>Email HTML Body</td>
        <td>
          <Well bsSize="small" className={styles.bodyPreview}>
            {notification.config.html_body_template || <em>Empty HTML body</em>}
          </Well>
        </td>
      </tr>
    </>
  </CommonNotificationSummary>
);

export default EmailNotificationSummary;
