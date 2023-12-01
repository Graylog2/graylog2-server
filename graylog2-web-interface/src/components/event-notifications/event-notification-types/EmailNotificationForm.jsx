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
import styled from 'styled-components';
import cloneDeep from 'lodash/cloneDeep';
import get from 'lodash/get';

import { IfPermitted, MultiSelect, SourceCodeEditor, TimezoneSelect } from 'components/common';
import UsersSelectField from 'components/users/UsersSelectField';
import { Col, ControlLabel, FormGroup, HelpBlock, Input, Row } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import HideOnCloud from 'util/conditional/HideOnCloud';

import './lookup-table-provider';

// TODO: Default body template should come from the server
const DEFAULT_BODY_TEMPLATE = `--- [Event Definition] ---------------------------
Title:       \${event_definition_title}
Description: \${event_definition_description}
Type:        \${event_definition_type}
--- [Event] --------------------------------------
Alert Replay:         \${http_external_uri}alerts/\${event.id}/replay-search
Timestamp:            \${event.timestamp}
Message:              \${event.message}
Source:               \${event.source}
Key:                  \${event.key}
Priority:             \${event.priority}
Alert:                \${event.alert}
Timestamp Processing: \${event.timestamp}
Timerange Start:      \${event.timerange_start}
Timerange End:        \${event.timerange_end}
Fields:
\${foreach event.fields field}  \${field.key}: \${field.value}
\${end}
\${if backlog}
--- [Backlog] ------------------------------------
Last messages accounting for this alert:
\${foreach backlog message}
\${message}
\${end}
\${end}
`;

const DEFAULT_HTML_BODY_TEMPLATE = `<table width="100%" border="0" cellpadding="10" cellspacing="0" style="background-color:#f9f9f9;border:none;line-height:1.2"><tbody>
<tr style="line-height:1.5"><th colspan="2" style="background-color:#e6e6e6">Event Definition</th></tr>
<tr><td width="200px">Title</td><td>\${event_definition_title}</td></tr>
<tr><td>Description</td><td>\${event_definition_description}</td></tr>
<tr><td>Type</td><td>\${event_definition_type}</td></tr>
</tbody></table>
<br /><table width="100%" border="0" cellpadding="10" cellspacing="0" style="background-color:#f9f9f9;border:none;line-height:1.2"><tbody>
<tr><th colspan="2" style="background-color:#e6e6e6;line-height:1.5">Event</th></tr>
<tr><td>Alert Replay</td><td>\${http_external_uri}alerts/\${event.id}/replay-search</td></tr>
<tr><td width="200px">Timestamp</td><td>\${event.timestamp}</td></tr>
<tr><td>Message</td><td>\${event.message}</td></tr>
<tr><td>Source</td><td>\${event.source}</td></tr>
<tr><td>Key</td><td>\${event.key}</td></tr>
<tr><td>Priority</td><td>\${event.priority}</td></tr>
<tr><td>Alert</td><td>\${event.alert}</td></tr>
<tr><td>Timestamp Processing</td><td>\${event.timestamp}</td></tr>
<tr><td>Timerange Start</td><td>\${event.timerange_start}</td></tr>
<tr><td>Timerange End</td><td>\${event.timerange_end}</td></tr>
<tr><td>Source Streams</td><td>\${event.source_streams}</td></tr>
<tr><td>Fields</td><td><ul style="list-style-type:square;">\${foreach event.fields field}<li>\${field.key}:\${field.value}</li>\${end}<ul></td></tr>
</tbody></table>
\${if backlog}
<br /><table width="100%" border="0" cellpadding="10" cellspacing="0" style="background-color:#f9f9f9;border:none;line-height:1.2"><tbody>
<tr><th style="background-color:#e6e6e6;line-height:1.5">Backlog (Last messages accounting for this alert)</th></tr>
\${foreach backlog message}
<tr><td>\${message}</td></tr>
\${end}
</tbody></table>
\${end}
`;

const EmailLookupRow = styled(Row)`
  margin-bottom: -9px;
`;

class EmailNotificationForm extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  static defaultConfig = {
    sender: '', // TODO: Default sender should come from the server. The default should be empty or the address configured in the email server settings
    // eslint-disable-next-line no-template-curly-in-string
    subject: 'Graylog event notification: ${event_definition_title}', // TODO: Default subject should come from the server
    reply_to: '',
    body_template: DEFAULT_BODY_TEMPLATE, // TODO: Default body template should come from the server
    html_body_template: DEFAULT_HTML_BODY_TEMPLATE,
    user_recipients: [],
    email_recipients: [],
    time_zone: 'UTC',
    lookup_recipient_emails: false,
    recipients_lut_name: null,
    recipients_lut_key: null,
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

  handleBodyTemplateChange = (nextValue) => {
    this.propagateChange('body_template', nextValue);
  };

  handleHtmlBodyTemplateChange = (nextValue) => {
    this.propagateChange('html_body_template', nextValue);
  };

  handleRecipientsChange = (key) => (nextValue) => this.propagateChange(key, nextValue === '' ? [] : nextValue.split(','));

  emailRecipientsFormGroup = () => {
    const { config, validation } = this.props;

    return (
      <FormGroup controlId="notification-email-recipients"
                 validationState={validation.errors.recipients ? 'error' : null}><ControlLabel>Email recipient(s) <small className="text-muted">(Optional)</small></ControlLabel>
        <MultiSelect id="notification-email-recipients"
                     value={Array.isArray(config.email_recipients) ? config.email_recipients.join(',') : ''}
                     addLabelText={'Add email "{label}"?'}
                     placeholder="Type email address"
                     options={[]}
                     onChange={this.handleRecipientsChange('email_recipients')}
                     allowCreate />
        <HelpBlock>
          {get(validation, 'errors.recipients[0]', 'Add email addresses that will receive this Notification.')}
        </HelpBlock>
      </FormGroup>
    );
  };

  emailLookupFormGroup = () => {
    const { config, validation } = this.props;

    return (
      <EmailLookupRow>
        <Col md={6}>
          <Input id="lookup-table-name"
                 name="recipients_lut_name"
                 label="Lookup Table Name"
                 type="text"
                 bsStyle={validation.errors.recipients_lut_name ? 'error' : null}
                 help={get(validation, 'errors.recipients_lut_name[0]', 'The lookup table name to search for email recipients.')}
                 value={config.recipients_lut_name || ''}
                 onChange={this.handleChange}
                 required />
        </Col>
        <Col md={6}>
          <Input id="lookup-table-key"
                 name="recipients_lut_key"
                 label="Lookup Table Key"
                 type="text"
                 bsStyle={validation.errors.recipients_lut_key ? 'error' : null}
                 help={get(validation, 'errors.recipients_lut_key[0]', 'The lookup table key to use when looking up email recipients.')}
                 value={config.recipients_lut_key || ''}
                 onChange={this.handleChange}
                 required />
        </Col>
      </EmailLookupRow>
    );
  };

  render() {
    const { config, validation } = this.props;

    return (
      <>
        <Input id="notification-subject"
               name="subject"
               label="Subject"
               type="text"
               bsStyle={validation.errors.subject ? 'error' : null}
               help={get(validation, 'errors.subject[0]', 'The subject that should be used for the email notification.')}
               value={config.subject || ''}
               onChange={this.handleChange}
               required />
        <Input id="notification-replyto"
               name="reply_to"
               label="Reply-To (Optional)"
               type="text"
               bsStyle={validation.errors.replyto ? 'error' : null}
               help={get(validation, 'errors.reply_to[0]', 'The email address that recipients should use for replies.')}
               value={config.reply_to || ''}
               onChange={this.handleChange} />
        <HideOnCloud>
          <Input id="notification-sender"
                 name="sender"
                 label={<ControlLabel>Sender <small className="text-muted">(Optional)</small></ControlLabel>}
                 type="text"
                 bsStyle={validation.errors.sender ? 'error' : null}
                 help={get(validation, 'errors.sender[0]', 'The email address that should be used as the notification sender. Leave it empty to use the default sender address.')}
                 value={config.sender || ''}
                 onChange={this.handleChange} />
        </HideOnCloud>

        <IfPermitted permissions="users:list">
          <FormGroup controlId="notification-user-recipients"
                     validationState={validation.errors.recipients ? 'error' : null}>
            <ControlLabel>User recipient(s) <small className="text-muted">(Optional)</small></ControlLabel>
            <UsersSelectField id="notification-user-recipients"
                              value={Array.isArray(config.user_recipients) ? config.user_recipients.join(',') : ''}
                              onChange={this.handleRecipientsChange('user_recipients')} />
            <HelpBlock>
              {get(validation, 'errors.recipients[0]', 'Select Graylog users that will receive this Notification.')}
            </HelpBlock>
          </FormGroup>
        </IfPermitted>
        {config.lookup_recipient_emails ? this.emailLookupFormGroup() : this.emailRecipientsFormGroup()}
        <FormGroup>
          <Input type="checkbox"
                 id="lookup_recipient_emails"
                 name="lookup_recipient_emails"
                 label="Use lookup table for Email Recipients"
                 onChange={this.handleUseRecipientLookupChange}
                 checked={config.lookup_recipient_emails} />
        </FormGroup>
        <Input id="notification-time-zone"
               help="Time zone used for timestamps in the email body."
               label={<>Time zone for date/time values <small className="text-muted">(Optional)</small></>}>
          <TimezoneSelect className="timezone-select"
                          name="time_zone"
                          value={config.time_zone}
                          onChange={this.handleTimeZoneChange} />
        </Input>
        <FormGroup controlId="notification-body-template"
                   validationState={validation.errors.body ? 'error' : null}>
          <ControlLabel>Body Template</ControlLabel>
          <SourceCodeEditor id="notification-body-template"
                            mode="text"
                            theme="light"
                            value={config.body_template || ''}
                            onChange={this.handleBodyTemplateChange} />
          <HelpBlock>
            {get(validation, 'errors.body[0]', 'The template that will be used to generate the email body.')}
          </HelpBlock>
        </FormGroup>
        <FormGroup controlId="notification-body-template"
                   validationState={validation.errors.body ? 'error' : null}>
          <ControlLabel>HTML Body Template</ControlLabel>
          <SourceCodeEditor id="notification-html-body-template"
                            mode="text"
                            theme="light"
                            value={config.html_body_template || ''}
                            onChange={this.handleHtmlBodyTemplateChange} />
          <HelpBlock>
            {get(validation, 'errors.body[0]', 'The template that will be used to generate the email HTML body.')}
          </HelpBlock>
        </FormGroup>
      </>
    );
  }
}

export default EmailNotificationForm;
