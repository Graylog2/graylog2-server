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
import PropTypes from 'prop-types';

import { Well } from 'components/bootstrap';

import CommonNotificationSummary from './CommonNotificationSummary';
import styles from './EmailNotificationSummary.css';

const EmailNotificationSummary = ({ notification, ...otherProps }) => (
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

EmailNotificationSummary.propTypes = {
  type: PropTypes.string.isRequired,
  notification: PropTypes.object,
  definitionNotification: PropTypes.object.isRequired,
};

EmailNotificationSummary.defaultProps = {
  notification: {},
};

export default EmailNotificationSummary;
