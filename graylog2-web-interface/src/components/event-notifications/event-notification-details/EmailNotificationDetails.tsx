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
import { useTranslation } from 'react-i18next';

import { ReadOnlyFormGroup } from 'components/common';
import { Well } from 'components/bootstrap';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';

import styles from '../event-notification-types/EmailNotificationSummary.css';

type EmailNotificationDetailsProps = {
  notification: any;
};

const EmailNotificationDetails = ({ notification }: EmailNotificationDetailsProps) => {
  const { t } = useTranslation('event_notifications');
  const isSecurityLicenseValid = usePluggableLicenseCheck('/license/security')?.data?.valid;
  const yesNo = (value: unknown) => (value ? t('summary.yes') : t('summary.no'));
  const recipients = (
    <ReadOnlyFormGroup
      label={t('email_summary.email_recipients')}
      value={notification.config.email_recipients.join(', ') || t('email_summary.no_email_recipients')}
    />
  );
  const recipientLookupInfo = (
    <>
      <ReadOnlyFormGroup
        label={t('email_summary.recipients_lut_name')}
        value={notification.config.recipients_lut_name}
      />
      <ReadOnlyFormGroup label={t('email_summary.recipients_lut_key')} value={notification.config.recipients_lut_key} />
    </>
  );
  const ccEmails = (
    <ReadOnlyFormGroup
      label={t('email_summary.cc_emails')}
      value={notification.config.cc_emails.join(', ') || t('email_summary.no_cc_emails')}
    />
  );
  const ccLookupInfo = (
    <>
      <ReadOnlyFormGroup label={t('email_summary.cc_lut_name')} value={notification.config.cc_emails_lut_name} />
      <ReadOnlyFormGroup label={t('email_summary.cc_lut_key')} value={notification.config.cc_emails_lut_key} />
    </>
  );

  const bccEmails = (
    <ReadOnlyFormGroup
      label={t('email_summary.bcc_emails')}
      value={notification.config.bcc_emails.join(', ') || t('email_summary.no_bcc_emails')}
    />
  );
  const bccLookupInfo = (
    <>
      <ReadOnlyFormGroup label={t('email_summary.bcc_lut_name')} value={notification.config.bcc_emails_lut_name} />
      <ReadOnlyFormGroup label={t('email_summary.bcc_lut_key')} value={notification.config.bcc_emails_lut_key} />
    </>
  );
  const sender = <ReadOnlyFormGroup label={t('email_summary.sender')} value={notification.config.sender} />;
  const senderLookupInfo = (
    <>
      <ReadOnlyFormGroup label={t('email_details.sender_lut_name')} value={notification.config.sender_lut_name} />
      <ReadOnlyFormGroup label={t('email_details.sender_lut_key')} value={notification.config.sender_lut_key} />
    </>
  );
  const replyTo = <ReadOnlyFormGroup label={t('email_summary.reply_to')} value={notification.config.reply_to} />;
  const replyToLookupInfo = (
    <>
      <ReadOnlyFormGroup label={t('email_details.reply_to_lut_name')} value={notification.config.reply_to_lut_name} />
      <ReadOnlyFormGroup label={t('email_details.reply_to_lut_key')} value={notification.config.reply_to_lut_key} />
    </>
  );

  return (
    <>
      <ReadOnlyFormGroup label={t('email_summary.single_email')} value={yesNo(notification.config.single_email)} />
      <ReadOnlyFormGroup
        label={t('email_summary.use_lut_sender')}
        value={yesNo(notification.config.lookup_sender_email)}
      />
      {notification.config.lookup_sender_email ? senderLookupInfo : sender}
      <ReadOnlyFormGroup label={t('email_summary.subject')} value={notification.config.subject} />
      <ReadOnlyFormGroup
        label={t('email_summary.use_lut_reply_to')}
        value={yesNo(notification.config.lookup_reply_to_email)}
      />
      {notification.config.lookup_reply_to_email ? replyToLookupInfo : replyTo}
      <ReadOnlyFormGroup
        label={t('email_summary.user_recipients')}
        value={notification.config.user_recipients.join(', ') || t('email_summary.no_user_recipients')}
      />
      <ReadOnlyFormGroup
        label={t('email_summary.use_lut_recipients')}
        value={yesNo(notification.config.lookup_recipient_emails)}
      />
      {notification.config.lookup_recipient_emails ? recipientLookupInfo : recipients}
      <ReadOnlyFormGroup
        label={t('email_details.cc_users')}
        value={notification.config.cc_users.join(', ') || t('email_summary.no_cc_users')}
      />
      <ReadOnlyFormGroup label={t('email_summary.use_lut_cc')} value={yesNo(notification.config.lookup_cc_emails)} />
      {notification.config.lookup_cc_emails ? ccLookupInfo : ccEmails}
      <ReadOnlyFormGroup
        label={t('email_details.bcc_users')}
        value={notification.config.bcc_users.join(', ') || t('email_summary.no_bcc_users')}
      />
      <ReadOnlyFormGroup label={t('email_summary.use_lut_bcc')} value={yesNo(notification.config.lookup_bcc_emails)} />
      {notification.config.lookup_bcc_emails ? bccLookupInfo : bccEmails}
      <ReadOnlyFormGroup label={t('email_details.time_zone')} value={notification.config.time_zone} />
      <ReadOnlyFormGroup
        label={t('email_summary.body')}
        value={
          <Well bsSize="small" className={styles.bodyPreview}>
            {notification.config.body_template || <em>{t('email_summary.empty_body')}</em>}
          </Well>
        }
      />
      <ReadOnlyFormGroup
        label={t('email_summary.html_body')}
        value={
          <Well bsSize="small" className={styles.bodyPreview}>
            {notification.config.html_body_template || <em>{t('email_summary.empty_body')}</em>}
          </Well>
        }
      />
      {isSecurityLicenseValid && (
        <ReadOnlyFormGroup
          label={t('email_summary.include_event_procedure')}
          value={yesNo(notification.config.include_event_procedure)}
        />
      )}
    </>
  );
};

export default EmailNotificationDetails;
