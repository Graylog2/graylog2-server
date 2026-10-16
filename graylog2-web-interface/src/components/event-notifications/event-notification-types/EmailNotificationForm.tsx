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
import React, { useCallback, useEffect, useMemo, useRef } from 'react';
import cloneDeep from 'lodash/cloneDeep';

import i18n from 'i18n';
import { IfPermitted, MultiSelect, SourceCodeEditor, TimezoneSelect } from 'components/common';
import { LookupTableFields } from 'components/lookup-tables';
import UsersSelectField from 'components/users/UsersSelectField';
import { ControlLabel, FormGroup, HelpBlock, Input } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import HideOnCloud from 'util/conditional/HideOnCloud';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';
import usePluginEntities from 'hooks/usePluginEntities';
import {
  DEFAULT_BODY_TEMPLATE,
  DEFAULT_HTML_BODY_TEMPLATE,
} from 'components/event-notifications/event-notification-types/emailNotificationTemplates';

// eslint-disable-next-line no-template-curly-in-string
// The form is a class component, so texts are translated at render time.
const t = (key: string) => i18n.t(key, { ns: 'events' });

const LOOKUP_KEY_PLACEHOLDER_TEXT = '${event.group_by_fields.group_by_field}';

const EventProcedureCheckbox = ({ checked, onChange }) => {
  const {
    data: { valid: validSecurityLicense },
  } = usePluggableLicenseCheck('/license/security');

  if (!validSecurityLicense) {
    return null;
  }

  return (
    <FormGroup>
      <Input
        type="checkbox"
        id="include_event_procedure"
        name="include_event_procedure"
        label={t('notifications_email.include_event_procedure')}
        onChange={onChange}
        checked={checked}
      />
    </FormGroup>
  );
};

const EmailTemplatesRunner = ({
  config,
  onChange,
  resetKey = undefined,
}: {
  config: any;
  onChange: (next: any) => void;
  resetKey?: string | number | undefined;
}) => {
  const { data: { valid: validCustomizationLicense } = { valid: false } } = usePluggableLicenseCheck(
    '/license/enterprise/customization',
  );

  const entities = usePluginEntities('customization.emailTemplates');
  const providingEntity = useMemo(
    () => (entities ?? []).find((e: any) => typeof e?.hooks?.useEmailTemplate === 'function'),
    [entities],
  );

  const noopUseEmailTemplate = useCallback(() => ({ templateConfig: undefined }), []);
  const useEmailTemplateHook = (providingEntity?.hooks?.useEmailTemplate ?? noopUseEmailTemplate) as () => {
    templateConfig?: {
      override_defaults?: boolean;
      text_body?: string | null;
      html_body?: string | null;
    };
  };

  const { templateConfig } = useEmailTemplateHook() || {};

  const key = String(resetKey ?? 'default');

  // Re-apply when any of these change
  const sig =
    validCustomizationLicense && templateConfig
      ? `${key}|${templateConfig.override_defaults ? '1' : '0'}|${templateConfig.text_body ?? ''}|${templateConfig.html_body ?? ''}`
      : `${key}|no-license-or-config`;

  const lastSigRef = useRef<string>('init');

  useEffect(() => {
    if (!validCustomizationLicense || !templateConfig) return;

    if (lastSigRef.current === sig) return;

    const { override_defaults, text_body, html_body } = templateConfig;

    let next = config;
    let changed = false;

    if (override_defaults === true) {
      const nextCfg = { ...config };
      const trimmedBody = (config.body_template ?? '').trim();
      const trimmedHtml = (config.html_body_template ?? '').trim();

      const shouldOverrideBody =
        typeof text_body === 'string' &&
        (trimmedBody === '' || (config.body_template ?? '') === DEFAULT_BODY_TEMPLATE) &&
        text_body !== config.body_template;
      const shouldOverrideHtml =
        typeof html_body === 'string' &&
        (trimmedHtml === '' || (config.html_body_template ?? '') === DEFAULT_HTML_BODY_TEMPLATE) &&
        html_body !== config.html_body_template;

      if (shouldOverrideBody) {
        nextCfg.body_template = text_body;
        changed = true;
      }
      if (shouldOverrideHtml) {
        nextCfg.html_body_template = html_body;
        changed = true;
      }

      if (changed) next = nextCfg;
    } else {
      const trimmedBody = (config.body_template ?? '').trim();
      const trimmedHtml = (config.html_body_template ?? '').trim();
      const hasCustomBody =
        trimmedBody !== '' &&
        (config.body_template ?? '') !== DEFAULT_BODY_TEMPLATE &&
        trimmedBody !== DEFAULT_BODY_TEMPLATE;
      const hasCustomHtml =
        trimmedHtml !== '' &&
        (config.html_body_template ?? '') !== DEFAULT_HTML_BODY_TEMPLATE &&
        trimmedHtml !== DEFAULT_HTML_BODY_TEMPLATE;

      if (hasCustomBody || hasCustomHtml) {
        lastSigRef.current = sig;

        return;
      }

      const nextCfg = { ...config };

      if (trimmedBody === '') {
        nextCfg.body_template = DEFAULT_BODY_TEMPLATE;
        changed = true;
      }
      if (trimmedHtml === '') {
        nextCfg.html_body_template = DEFAULT_HTML_BODY_TEMPLATE;
        changed = true;
      }

      if (changed) next = nextCfg;
    }

    if (changed) onChange(next);

    lastSigRef.current = sig;
  }, [sig, validCustomizationLicense, templateConfig, config, onChange]);

  return null;
};

type EmailNotificationFormProps = {
  config: any;
  validation: any;
  onChange: (...args: any[]) => void;
};

class EmailNotificationForm extends React.Component<
  EmailNotificationFormProps,
  {
    [key: string]: any;
  }
> {
  static defaultConfig = {
    sender: '', // TODO: Default sender should come from the server. The default should be empty or the address configured in the email server settings
    // eslint-disable-next-line no-template-curly-in-string
    subject: 'Event notification: ${event_definition_title}', // TODO: Default subject should come from the server
    reply_to: '',
    body_template: DEFAULT_BODY_TEMPLATE, // TODO: Default body template should come from the server
    html_body_template: DEFAULT_HTML_BODY_TEMPLATE,
    user_recipients: [],
    email_recipients: [],
    time_zone: 'UTC',
    lookup_recipient_emails: false,
    recipients_lut_name: null,
    recipients_lut_key: null,
    lookup_sender_email: false,
    sender_lut_name: null,
    sender_lut_key: null,
    lookup_reply_to_email: false,
    reply_to_lut_name: null,
    reply_to_lut_key: null,
    single_email: false,
    cc_users: [],
    cc_emails: [],
    lookup_cc_emails: false,
    cc_emails_lut_name: null,
    cc_emails_lut_key: null,
    bcc_users: [],
    bcc_emails: [],
    lookup_bcc_emails: false,
    bcc_emails_lut_name: null,
    bcc_emails_lut_key: null,
    include_event_procedure: false,
  };

  propagateChange = (key, value) => {
    const { config, onChange } = this.props;
    const nextConfig = cloneDeep(config);

    nextConfig[key] = value;
    onChange(nextConfig);
  };

  handleChange = (event) => {
    const { name } = event.target;

    this.propagateChange(name, getValueFromInput(event.target));
  };

  handleSelectChange = (key, value) => {
    this.propagateChange(key, value);
  };

  handleTimeZoneChange = (nextValue) => {
    this.propagateChange('time_zone', nextValue);
  };

  handleUseRecipientLookupChange = (event) => {
    const { config, onChange } = this.props;
    const nextConfig = cloneDeep(config);
    const nextValue = getValueFromInput(event.target);
    nextConfig.lookup_recipient_emails = nextValue;

    if (nextValue) {
      nextConfig.email_recipients = [];
    } else {
      nextConfig.recipients_lut_name = null;
      nextConfig.recipients_lut_key = null;
    }

    onChange(nextConfig);
  };

  handleUseCcLookupChange = (event) => {
    const { config, onChange } = this.props;
    const nextConfig = cloneDeep(config);
    const nextValue = getValueFromInput(event.target);
    nextConfig.lookup_cc_emails = nextValue;

    if (nextValue) {
      nextConfig.cc_emails = [];
    } else {
      nextConfig.cc_emails_lut_name = null;
      nextConfig.cc_emails_lut_key = null;
    }

    onChange(nextConfig);
  };

  handleUseBccLookupChange = (event) => {
    const { config, onChange } = this.props;
    const nextConfig = cloneDeep(config);
    const nextValue = getValueFromInput(event.target);
    nextConfig.lookup_bcc_emails = nextValue;

    if (nextValue) {
      nextConfig.bcc_emails = [];
    } else {
      nextConfig.bcc_emails_lut_name = null;
      nextConfig.bcc_emails_lut_key = null;
    }

    onChange(nextConfig);
  };

  handleUseReplyToLookupChange = (event) => {
    const { config, onChange } = this.props;
    const nextConfig = cloneDeep(config);
    const nextValue = getValueFromInput(event.target);
    nextConfig.lookup_reply_to_email = nextValue;

    if (nextValue) {
      nextConfig.reply_to = '';
    } else {
      nextConfig.reply_to_lut_name = null;
      nextConfig.reply_to_lut_key = null;
    }

    onChange(nextConfig);
  };

  handleUseSenderLookupChange = (event) => {
    const { config, onChange } = this.props;
    const nextConfig = cloneDeep(config);
    const nextValue = getValueFromInput(event.target);
    nextConfig.lookup_sender_email = nextValue;

    if (nextValue) {
      nextConfig.sender = '';
    } else {
      nextConfig.sender_lut_name = null;
      nextConfig.sender_lut_key = null;
    }

    onChange(nextConfig);
  };

  handleBodyTemplateChange = (nextValue) => {
    this.propagateChange('body_template', nextValue);
  };

  handleHtmlBodyTemplateChange = (nextValue) => {
    this.propagateChange('html_body_template', nextValue);
  };

  handleRecipientsChange = (key) => (nextValue) =>
    this.propagateChange(key, nextValue === '' ? [] : nextValue.split(','));

  emailRecipientsFormGroup = () => {
    const { config, validation } = this.props;

    return (
      <FormGroup
        controlId="notification-email-recipients"
        validationState={validation.errors.recipients ? 'error' : null}>
        <ControlLabel>
          {t('notifications_email.email_recipients')} <small className="text-muted">{t('notifications_form.optional')}</small>
        </ControlLabel>
        <MultiSelect
          id="notification-email-recipients"
          value={Array.isArray(config.email_recipients) ? config.email_recipients.join(',') : ''}
          addLabelText={t('notifications_email.add_email')}
          placeholder={t('notifications_email.type_email')}
          options={[]}
          onChange={this.handleRecipientsChange('email_recipients')}
          allowCreate
        />
        <HelpBlock>
          {validation?.errors?.recipients?.[0] || t('notifications_email.email_recipients_help')}
        </HelpBlock>
      </FormGroup>
    );
  };

  emailLookupFormGroup = () => {
    const { config, validation } = this.props;

    const customKeyField = (
      <Input
        id="recipients-table-key"
        name="recipients_lut_key"
        label={t('notifications_email.recipients_lut_key')}
        type="text"
        placeholder={LOOKUP_KEY_PLACEHOLDER_TEXT}
        bsStyle={validation.errors.recipients_lut_key ? 'error' : null}
        help={
          validation?.errors?.recipients_lut_key?.[0] ||
          t('notifications_email.lut_key_help')
        }
        value={config.recipients_lut_key || ''}
        onChange={this.handleChange}
        required
      />
    );

    return (
      <LookupTableFields
        onTableNameChange={(value) => this.handleSelectChange('recipients_lut_name', value)}
        onKeyChange={(value) => this.handleSelectChange('recipients_lut_key', value)}
        selectedTableName={config.recipients_lut_name || ''}
        selectedKeyName={config.recipients_lut_key || ''}
        nameValidation={validation.errors.recipients_lut_name}
        keyValidation={validation.errors.recipients_lut_key}
        lookupTableNameLabel="Recipients Lookup Table Name"
        customKeyField={customKeyField}
      />
    );
  };

  ccEmailsFormGroup = () => {
    const { config, validation } = this.props;

    return (
      <FormGroup controlId="notification-cc-emails" validationState={validation.errors.cc_emails ? 'error' : null}>
        <ControlLabel>
          {t('notifications_email.cc_emails')} <small className="text-muted">{t('notifications_form.optional')}</small>
        </ControlLabel>
        <MultiSelect
          id="notification-cc-email"
          value={Array.isArray(config.cc_emails) ? config.cc_emails.join(',') : ''}
          addLabelText={t('notifications_email.add_email')}
          placeholder={t('notifications_email.type_email')}
          options={[]}
          onChange={this.handleRecipientsChange('cc_emails')}
          allowCreate
        />
        <HelpBlock>
          {validation?.errors?.cc_emails?.[0] ||
            t('notifications_email.cc_emails_help')}
        </HelpBlock>
      </FormGroup>
    );
  };

  ccLookupFormGroup = () => {
    const { config, validation } = this.props;

    const customKeyField = (
      <Input
        id="cc-emails-table-key"
        name="cc_emails_lut_key"
        label={t('notifications_email.cc_lut_key')}
        type="text"
        placeholder={LOOKUP_KEY_PLACEHOLDER_TEXT}
        bsStyle={validation.errors.cc_emails_lut_key ? 'error' : null}
        help={
          validation?.errors?.cc_emails_lut_key?.[0] || t('notifications_email.lut_key_help')
        }
        value={config.cc_emails_lut_key || ''}
        onChange={this.handleChange}
        required
      />
    );

    return (
      <LookupTableFields
        onTableNameChange={(value) => this.handleSelectChange('cc_emails_lut_name', value)}
        onKeyChange={(value) => this.handleSelectChange('cc_emails_lut_key', value)}
        selectedTableName={config.cc_emails_lut_name || ''}
        selectedKeyName={config.cc_emails_lut_key || ''}
        nameValidation={validation.errors.cc_emails_lut_name}
        keyValidation={validation.errors.cc_emails_lut_key}
        lookupTableNameLabel="CC Emails Lookup Table Name"
        customKeyField={customKeyField}
      />
    );
  };

  bccEmailsFormGroup = () => {
    const { config, validation } = this.props;

    return (
      <FormGroup controlId="notification-bcc-emails" validationState={validation.errors.bcc_emails ? 'error' : null}>
        <ControlLabel>
          {t('notifications_email.bcc_emails')} <small className="text-muted">{t('notifications_form.optional')}</small>
        </ControlLabel>
        <MultiSelect
          id="notification-bcc-email"
          value={Array.isArray(config.bcc_emails) ? config.bcc_emails.join(',') : ''}
          addLabelText={t('notifications_email.add_email')}
          placeholder={t('notifications_email.type_email')}
          options={[]}
          onChange={this.handleRecipientsChange('bcc_emails')}
          allowCreate
        />
        <HelpBlock>
          {validation?.errors?.bcc_emails?.[0] ||
            t('notifications_email.bcc_emails_help')}
        </HelpBlock>
      </FormGroup>
    );
  };

  bccLookupFormGroup = () => {
    const { config, validation } = this.props;

    const customKeyField = (
      <Input
        id="bcc-emails-table-key"
        name="bcc_emails_lut_key"
        label={t('notifications_email.bcc_lut_key')}
        type="text"
        placeholder={LOOKUP_KEY_PLACEHOLDER_TEXT}
        bsStyle={validation.errors.bcc_emails_lut_key ? 'error' : null}
        help={
          validation?.errors?.bcc_emails_lut_key?.[0] ||
          t('notifications_email.lut_key_help')
        }
        value={config.bcc_emails_lut_key || ''}
        onChange={this.handleChange}
        required
      />
    );

    return (
      <LookupTableFields
        onTableNameChange={(value) => this.handleSelectChange('bcc_emails_lut_name', value)}
        onKeyChange={(value) => this.handleSelectChange('bcc_emails_lut_key', value)}
        selectedTableName={config.bcc_emails_lut_name || ''}
        selectedKeyName={config.bcc_emails_lut_key || ''}
        nameValidation={validation.errors.bcc_emails_lut_name}
        keyValidation={validation.errors.bcc_emails_lut_key}
        lookupTableNameLabel="BCC Emails Lookup Table Name"
        customKeyField={customKeyField}
      />
    );
  };

  senderInput = () => {
    const { config, validation } = this.props;

    return (
      <Input
        id="notification-sender"
        name="sender"
        label={
          <ControlLabel>
            {t('notifications_email.sender')} <small className="text-muted">{t('notifications_form.optional')}</small>
          </ControlLabel>
        }
        type="text"
        bsStyle={validation.errors.sender ? 'error' : null}
        help={
          validation?.errors?.sender?.[0] ||
          t('notifications_email.sender_help')
        }
        value={config.sender || ''}
        onChange={this.handleChange}
      />
    );
  };

  senderLookupFormGroup = () => {
    const { config, validation } = this.props;

    const customKeyField = (
      <Input
        id="sender-lookup-table-key"
        name="sender_lut_key"
        label={t('notifications_email.sender_lut_key')}
        type="text"
        placeholder={LOOKUP_KEY_PLACEHOLDER_TEXT}
        bsStyle={validation.errors.sender_lut_key ? 'error' : null}
        help={
          validation?.errors?.sender_lut_key?.[0] || t('notifications_email.lut_key_help')
        }
        value={config.sender_lut_key || ''}
        onChange={this.handleChange}
        required
      />
    );

    return (
      <LookupTableFields
        onTableNameChange={(value) => this.handleSelectChange('sender_lut_name', value)}
        onKeyChange={(value) => this.handleSelectChange('sender_lut_key', value)}
        selectedTableName={config.sender_lut_name || ''}
        selectedKeyName={config.sender_lut_key || ''}
        nameValidation={validation.errors.sender_lut_name}
        keyValidation={validation.errors.sender_lut_key}
        lookupTableNameLabel="Sender Lookup Table Name"
        customKeyField={customKeyField}
      />
    );
  };

  replyToInput = () => {
    const { config, validation } = this.props;

    return (
      <Input
        id="notification-replyto"
        name="reply_to"
        label={t('notifications_email.reply_to_optional')}
        type="text"
        bsStyle={validation.errors.replyto ? 'error' : null}
        help={validation?.errors?.reply_to?.[0] || t('notifications_email.reply_to_help')}
        value={config.reply_to || ''}
        onChange={this.handleChange}
      />
    );
  };

  replyToLookupFormGroup = () => {
    const { config, validation } = this.props;

    const customKeyField = (
      <Input
        id="reply-to-lookup-table-key"
        name="reply_to_lut_key"
        label={t('notifications_email.reply_to_lut_key')}
        type="text"
        placeholder={LOOKUP_KEY_PLACEHOLDER_TEXT}
        bsStyle={validation.errors.reply_to_lut_key ? 'error' : null}
        help={
          validation?.errors?.reply_to_lut_key?.[0] || t('notifications_email.lut_key_help')
        }
        value={config.reply_to_lut_key || ''}
        onChange={this.handleChange}
        required
      />
    );

    return (
      <LookupTableFields
        onTableNameChange={(value) => this.handleSelectChange('reply_to_lut_name', value)}
        onKeyChange={(value) => this.handleSelectChange('reply_to_lut_key', value)}
        selectedTableName={config.reply_to_lut_name || ''}
        selectedKeyName={config.reply_to_lut_key || ''}
        nameValidation={validation.errors.reply_to_lut_name}
        keyValidation={validation.errors.reply_to_lut_key}
        lookupTableNameLabel="Reply To Lookup Table Name"
        customKeyField={customKeyField}
      />
    );
  };

  render() {
    const { config, validation, onChange } = this.props;

    return (
      <>
        <EmailTemplatesRunner config={config} onChange={onChange} resetKey={config?.type || config?.id} />
        <Input
          id="notification-subject"
          name="subject"
          label={t('notifications_email.subject')}
          type="text"
          bsStyle={validation.errors.subject ? 'error' : null}
          help={validation?.errors?.subject?.[0] || t('notifications_email.subject_help')}
          value={config.subject || ''}
          onChange={this.handleChange}
          required
        />
        <FormGroup>
          <Input
            type="checkbox"
            id="single_email"
            name="single_email"
            label={t('notifications_email.single_email')}
            onChange={this.handleChange}
            checked={config.single_email}
          />
        </FormGroup>
        {config.lookup_reply_to_email ? this.replyToLookupFormGroup() : this.replyToInput()}
        <FormGroup>
          <Input
            type="checkbox"
            id="lookup_reply_to_email"
            name="lookup_reply_to_email"
            label={t('notifications_email.use_lut_reply_to')}
            onChange={this.handleUseReplyToLookupChange}
            checked={config.lookup_reply_to_email}
          />
        </FormGroup>
        <HideOnCloud>
          {config.lookup_sender_email ? this.senderLookupFormGroup() : this.senderInput()}
          <FormGroup>
            <Input
              type="checkbox"
              id="lookup_sender_email"
              name="lookup_sender_email"
              label={t('notifications_email.use_lut_sender')}
              onChange={this.handleUseSenderLookupChange}
              checked={config.lookup_sender_email}
            />
          </FormGroup>
        </HideOnCloud>

        <IfPermitted permissions="users:list">
          <FormGroup
            controlId="notification-user-recipients"
            validationState={validation.errors.recipients ? 'error' : null}>
            <ControlLabel>
              {t('notifications_email.user_recipients')} <small className="text-muted">{t('notifications_form.optional')}</small>
            </ControlLabel>
            <UsersSelectField
              value={Array.isArray(config.user_recipients) ? config.user_recipients.join(',') : ''}
              onChange={this.handleRecipientsChange('user_recipients')}
            />
            <HelpBlock>
              {validation?.errors?.recipients?.[0] || t('notifications_email.user_recipients_help')}
            </HelpBlock>
          </FormGroup>
        </IfPermitted>
        {config.lookup_recipient_emails ? this.emailLookupFormGroup() : this.emailRecipientsFormGroup()}
        <FormGroup>
          <Input
            type="checkbox"
            id="lookup_recipient_emails"
            name="lookup_recipient_emails"
            label={t('notifications_email.use_lut_recipients')}
            onChange={this.handleUseRecipientLookupChange}
            checked={config.lookup_recipient_emails}
          />
        </FormGroup>

        <IfPermitted permissions="users:list">
          <FormGroup controlId="notification-cc-users" validationState={validation.errors.cc_users ? 'error' : null}>
            <ControlLabel>
              {t('notifications_email.cc_users')} <small className="text-muted">{t('notifications_form.optional')}</small>
            </ControlLabel>
            <UsersSelectField
              value={Array.isArray(config.cc_users) ? config.cc_users.join(',') : ''}
              onChange={this.handleRecipientsChange('cc_users')}
            />
            <HelpBlock>
              {validation?.errors?.cc_users?.[0] || t('notifications_email.cc_users_help')}
            </HelpBlock>
          </FormGroup>
        </IfPermitted>
        {config.lookup_cc_emails ? this.ccLookupFormGroup() : this.ccEmailsFormGroup()}
        <FormGroup>
          <Input
            type="checkbox"
            id="lookup_cc_emails"
            name="lookup_cc_emails"
            label={t('notifications_email.use_lut_cc')}
            onChange={this.handleUseCcLookupChange}
            checked={config.lookup_cc_emails}
          />
        </FormGroup>

        <IfPermitted permissions="users:list">
          <FormGroup controlId="notification-bcc-users" validationState={validation.errors.bcc_users ? 'error' : null}>
            <ControlLabel>
              {t('notifications_email.bcc_users')} <small className="text-muted">{t('notifications_form.optional')}</small>
            </ControlLabel>
            <UsersSelectField
              value={Array.isArray(config.bcc_users) ? config.bcc_users.join(',') : ''}
              onChange={this.handleRecipientsChange('bcc_users')}
            />
            <HelpBlock>
              {validation?.errors?.bcc_users?.[0] || t('notifications_email.bcc_users_help')}
            </HelpBlock>
          </FormGroup>
        </IfPermitted>
        {config.lookup_bcc_emails ? this.bccLookupFormGroup() : this.bccEmailsFormGroup()}
        <FormGroup>
          <Input
            type="checkbox"
            id="lookup_bcc_emails"
            name="lookup_bcc_emails"
            label={t('notifications_email.use_lut_bcc')}
            onChange={this.handleUseBccLookupChange}
            checked={config.lookup_bcc_emails}
          />
        </FormGroup>

        <Input
          id="notification-time-zone"
          help={t('notifications_email.time_zone_help')}
          label={
            <>
              {t('notifications_email.time_zone')} <small className="text-muted">{t('notifications_form.optional')}</small>
            </>
          }>
          <TimezoneSelect
            className="timezone-select"
            name="time_zone"
            value={config.time_zone}
            onChange={this.handleTimeZoneChange}
          />
        </Input>
        <FormGroup controlId="notification-body-template" validationState={validation.errors.body ? 'error' : null}>
          <ControlLabel>{t('notifications_email.body_template')}</ControlLabel>
          <SourceCodeEditor
            id="notification-body-template"
            mode="text"
            theme="light"
            value={config.body_template || ''}
            onChange={this.handleBodyTemplateChange}
          />
          <HelpBlock>
            {validation?.errors?.body?.[0] || t('notifications_email.body_template_help')}
          </HelpBlock>
        </FormGroup>
        <FormGroup controlId="notification-body-template" validationState={validation.errors.body ? 'error' : null}>
          <ControlLabel>{t('notifications_email.html_body_template')}</ControlLabel>
          <SourceCodeEditor
            id="notification-html-body-template"
            mode="text"
            theme="light"
            value={config.html_body_template || ''}
            onChange={this.handleHtmlBodyTemplateChange}
          />
          <HelpBlock>
            {validation?.errors?.body?.[0] || t('notifications_email.html_body_template_help')}
          </HelpBlock>
        </FormGroup>
        <EventProcedureCheckbox checked={config.include_event_procedure} onChange={this.handleChange} />
      </>
    );
  }
}

export default EmailNotificationForm;
