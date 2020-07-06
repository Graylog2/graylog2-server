import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { ControlLabel, FormGroup, HelpBlock } from 'components/graylog';
import { MultiSelect, SourceCodeEditor } from 'components/common';
import { Input } from 'components/bootstrap';
import FormsUtils from 'util/FormsUtils';

// TODO: Default body template should come from the server
const DEFAULT_BODY_TEMPLATE = `--- [Event Definition] ---------------------------
Title:       \${event_definition_title}
Description: \${event_definition_description}
Type:        \${event_definition_type}
--- [Event] --------------------------------------
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

class EmailNotificationForm extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    users: PropTypes.array.isRequired,
  };

  static defaultConfig = {
    sender: '', // TODO: Default sender should come from the server. The default should be empty or the address configured in the email server settings
    // eslint-disable-next-line no-template-curly-in-string
    subject: 'Graylog event notification: ${event_definition_title}', // TODO: Default subject should come from the server
    body_template: DEFAULT_BODY_TEMPLATE, // TODO: Default body template should come from the server
    user_recipients: [],
    email_recipients: [],
  };

  propagateChange = (key, value) => {
    const { config, onChange } = this.props;
    const nextConfig = lodash.cloneDeep(config);

    nextConfig[key] = value;
    onChange(nextConfig);
  };

  handleChange = (event) => {
    const { name } = event.target;

    this.propagateChange(name, FormsUtils.getValueFromInput(event.target));
  };

  handleBodyTemplateChange = (nextValue) => {
    this.propagateChange('body_template', nextValue);
  };

  handleRecipientsChange = (key) => {
    return (nextValue) => this.propagateChange(key, nextValue === '' ? [] : nextValue.split(','));
  };

  formatUsers = (users) => {
    return users.map((user) => ({ label: `${user.username} (${user.full_name})`, value: user.username }));
  };

  render() {
    const { config, users, validation } = this.props;

    return (
      <>
        <Input id="notification-sender"
               name="sender"
               label="Sender"
               type="text"
               bsStyle={validation.errors.sender ? 'error' : null}
               help={lodash.get(validation, 'errors.sender[0]', 'The email address that should be used as the notification sender.')}
               value={config.sender || ''}
               onChange={this.handleChange}
               required />
        <Input id="notification-subject"
               name="subject"
               label="Subject"
               type="text"
               bsStyle={validation.errors.subject ? 'error' : null}
               help={lodash.get(validation, 'errors.subject[0]', 'The subject that should be used for the email notification.')}
               value={config.subject || ''}
               onChange={this.handleChange}
               required />
        <FormGroup controlId="notification-user-recipients"
                   validationState={validation.errors.recipients ? 'error' : null}>
          <ControlLabel>User recipient(s) <small className="text-muted">(Optional)</small></ControlLabel>
          <MultiSelect id="notification-user-recipients"
                       value={Array.isArray(config.user_recipients) ? config.user_recipients.join(',') : ''}
                       placeholder="Select user(s)..."
                       options={this.formatUsers(users)}
                       onChange={this.handleRecipientsChange('user_recipients')} />
          <HelpBlock>
            {lodash.get(validation, 'errors.recipients[0]', 'Select Graylog users that will receive this Notification.')}
          </HelpBlock>
        </FormGroup>

        <FormGroup controlId="notification-email-recipients"
                   validationState={validation.errors.recipients ? 'error' : null}>
          <ControlLabel>Email recipient(s) <small className="text-muted">(Optional)</small></ControlLabel>
          <MultiSelect id="notification-email-recipients"
                       value={Array.isArray(config.email_recipients) ? config.email_recipients.join(',') : ''}
                       addLabelText={'Add email "{label}"?'}
                       placeholder="Type email address"
                       options={[]}
                       onChange={this.handleRecipientsChange('email_recipients')}
                       allowCreate />
          <HelpBlock>
            {lodash.get(validation, 'errors.recipients[0]', 'Add email addresses that will receive this Notification.')}
          </HelpBlock>
        </FormGroup>
        <FormGroup controlId="notification-body-template"
                   validationState={validation.errors.body_template ? 'error' : null}>
          <ControlLabel>Body Template</ControlLabel>
          <SourceCodeEditor id="notification-body-template"
                            mode="text"
                            theme="light"
                            value={config.body_template || ''}
                            onChange={this.handleBodyTemplateChange} />
          <HelpBlock>
            {lodash.get(validation, 'errors.body_template[0]', 'The template that will be used to generate the email body.')}
          </HelpBlock>
        </FormGroup>
      </>
    );
  }
}

export default EmailNotificationForm;
