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
import { useTranslation } from 'react-i18next';

import { Well } from 'components/bootstrap';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';

import CommonNotificationSummary from './CommonNotificationSummary';
import styles from './EmailNotificationSummary.css';

type EmailNotificationSummaryProps = {
  type: string;
  notification?: any;
  definitionNotification: any;
};

const EmailNotificationSummary = ({ notification = {}, ...otherProps }: EmailNotificationSummaryProps) => {
  const { t } = useTranslation('event_notifications');
  const isSecurityLicenseValid = usePluggableLicenseCheck('/license/security')?.data?.valid;
  const yesNo = (value: unknown) => (value ? t('summary.yes') : t('summary.no'));

  return (
    <CommonNotificationSummary notification={notification} {...otherProps}>
      <>
        <tr>
          <td>{t('email_summary.use_lut_sender')}</td>
          <td>{yesNo(notification.config.lookup_sender_email)}</td>
        </tr>
        {notification.config.lookup_sender_email ? (
          <>
            <tr>
              <td>{t('email_summary.sender_lut_name')}</td>
              <td>{notification.config.sender_lut_name}</td>
            </tr>
            <tr>
              <td>{t('email_summary.sender_lut_key')}</td>
              <td>{notification.config.sender_lut_key}</td>
            </tr>
          </>
        ) : (
          <tr>
            <td>{t('email_summary.sender')}</td>
            <td>{notification.config.sender}</td>
          </tr>
        )}
        <tr>
          <td>{t('email_summary.subject')}</td>
          <td>{notification.config.subject}</td>
        </tr>
        <tr>
          <td>{t('email_summary.single_email')}</td>
          <td>{notification.config.single_email}</td>
        </tr>
        <tr>
          <td>{t('email_summary.use_lut_reply_to')}</td>
          <td>{yesNo(notification.config.lookup_reply_to_email)}</td>
        </tr>
        {notification.config.lookup_reply_to_email ? (
          <>
            <tr>
              <td>{t('email_summary.reply_to_lut_name')}</td>
              <td>{notification.config.reply_to_lut_name}</td>
            </tr>
            <tr>
              <td>{t('email_summary.reply_to_lut_key')}</td>
              <td>{notification.config.reply_to_lut_key}</td>
            </tr>
          </>
        ) : (
          <tr>
            <td>{t('email_summary.reply_to')}</td>
            <td>{notification.config.reply_to}</td>
          </tr>
        )}

        <tr>
          <td>{t('email_summary.user_recipients')}</td>
          <td>{notification.config.user_recipients.join(', ') || t('email_summary.no_user_recipients')}</td>
        </tr>
        <tr>
          <td>{t('email_summary.use_lut_recipients')}</td>
          <td>{yesNo(notification.config.lookup_recipient_emails)}</td>
        </tr>
        {notification.config.lookup_recipient_emails ? (
          <>
            <tr>
              <td>{t('email_summary.recipients_lut_name')}</td>
              <td>{notification.config.recipients_lut_name}</td>
            </tr>
            <tr>
              <td>{t('email_summary.recipients_lut_key')}</td>
              <td>{notification.config.recipients_lut_key}</td>
            </tr>
          </>
        ) : (
          <tr>
            <td>{t('email_summary.email_recipients')}</td>
            <td>{notification.config.email_recipients.join(', ') || t('email_summary.no_email_recipients')}</td>
          </tr>
        )}

        <tr>
          <td>{t('email_summary.cc_users')}</td>
          <td>{notification.config.cc_users.join(', ') || t('email_summary.no_cc_users')}</td>
        </tr>
        <tr>
          <td>{t('email_summary.use_lut_cc')}</td>
          <td>{yesNo(notification.config.lookup_cc_emails)}</td>
        </tr>
        {notification.config.lookup_cc_emails ? (
          <>
            <tr>
              <td>{t('email_summary.cc_lut_name')}</td>
              <td>{notification.config.cc_emails_lut_name}</td>
            </tr>
            <tr>
              <td>{t('email_summary.cc_lut_key')}</td>
              <td>{notification.config.cc_emails_lut_key}</td>
            </tr>
          </>
        ) : (
          <tr>
            <td>{t('email_summary.cc_emails')}</td>
            <td>{notification.config.cc_emails.join(', ') || t('email_summary.no_cc_emails')}</td>
          </tr>
        )}

        <tr>
          <td>{t('email_summary.bcc_users')}</td>
          <td>{notification.config.bcc_users.join(', ') || t('email_summary.no_bcc_users')}</td>
        </tr>
        <tr>
          <td>{t('email_summary.use_lut_bcc')}</td>
          <td>{yesNo(notification.config.lookup_bcc_emails)}</td>
        </tr>
        {notification.config.lookup_bcc_emails ? (
          <>
            <tr>
              <td>{t('email_summary.bcc_lut_name')}</td>
              <td>{notification.config.bcc_emails_lut_name}</td>
            </tr>
            <tr>
              <td>{t('email_summary.bcc_lut_key')}</td>
              <td>{notification.config.bcc_emails_lut_key}</td>
            </tr>
          </>
        ) : (
          <tr>
            <td>{t('email_summary.bcc_emails')}</td>
            <td>{notification.config.bcc_emails.join(', ') || t('email_summary.no_bcc_emails')}</td>
          </tr>
        )}

        <tr>
          <td>{t('email_summary.body')}</td>
          <td>
            <Well bsSize="small" className={styles.bodyPreview}>
              {notification.config.body_template || <em>{t('email_summary.empty_body')}</em>}
            </Well>
          </td>
        </tr>
        <tr>
          <td>{t('email_summary.html_body')}</td>
          <td>
            <Well bsSize="small" className={styles.bodyPreview}>
              {notification.config.html_body_template || <em>{t('email_summary.empty_html_body')}</em>}
            </Well>
          </td>
        </tr>
        {isSecurityLicenseValid && (
          <tr>
            <td>{t('email_summary.include_event_procedure')}</td>
            <td>{yesNo(notification.config.include_event_procedure)}</td>
          </tr>
        )}
      </>
    </CommonNotificationSummary>
  );
};

export default EmailNotificationSummary;
