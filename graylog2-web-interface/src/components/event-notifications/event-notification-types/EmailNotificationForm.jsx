import React from 'react';
import PropTypes from 'prop-types';
import { ControlLabel, FormGroup, HelpBlock } from 'react-bootstrap';
import lodash from 'lodash';

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
    onChange: PropTypes.func.isRequired,
    users: PropTypes.array.isRequired,
  };

  componentDidMount() {
    // Set initial config for this type
    const { config, onChange } = this.props;
    const initialConfig = {
      sender: 'graylog@example.org', // TODO: Default sender should come from the server
      // eslint-disable-next-line no-template-curly-in-string
      subject: 'Graylog event notification: ${event_definition_title}', // TODO: Default subject should come from the server
      body_template: DEFAULT_BODY_TEMPLATE, // TODO: Default body template should come from the server
      user_recipients: [],
      email_recipients: [],
    };
    onChange(Object.assign({}, initialConfig, config));
  }

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
    return nextValue => this.propagateChange(key, nextValue === '' ? [] : nextValue.split(','));
  };

  formatUsers = (users) => {
    return users.map(user => ({ label: `${user.username} (${user.full_name})`, value: user.username }));
  };

  render() {
    const { config, users } = this.props;

    return (
      <React.Fragment>
        <Input id="notification-sender"
               name="sender"
               label="Sender"
               type="text"
               help="The email address that should be used as the notification sender."
               value={config.sender || ''}
               onChange={this.handleChange}
               required />
        <Input id="notification-subject"
               name="subject"
               label="Subject"
               type="text"
               help="The subject that should be used for the email notification."
               value={config.subject || ''}
               onChange={this.handleChange}
               required />
        <FormGroup id="notification-user-recipients">
          <ControlLabel>User recipient(s) <small className="text-muted">(Optional)</small></ControlLabel>
          <MultiSelect id="notification-user-recipients"
                       value={Array.isArray(config.user_recipients) ? config.user_recipients.join(',') : ''}
                       placeholder="Select user(s)..."
                       options={this.formatUsers(users)}
                       onChange={this.handleRecipientsChange('user_recipients')} />
          <HelpBlock>Select Graylog users that will receive this Notification.</HelpBlock>
        </FormGroup>

        <FormGroup id="notification-email-recipients">
          <ControlLabel>Email recipient(s) <small className="text-muted">(Optional)</small></ControlLabel>
          <MultiSelect id="notification-email-recipients"
                       value={Array.isArray(config.email_recipients) ? config.email_recipients.join(',') : ''}
                       addLabelText={'Add email "{label}"?'}
                       placeholder="Type email address"
                       options={[]}
                       onChange={this.handleRecipientsChange('email_recipients')}
                       allowCreate />
          <HelpBlock>Add email addresses that will receive this Notification.</HelpBlock>
        </FormGroup>
        <FormGroup controlId="notification-body-template">
          <ControlLabel>Body Template</ControlLabel>
          <SourceCodeEditor id="notification-body-template"
                            mode="text"
                            theme="light"
                            value={config.body_template || ''}
                            onChange={this.handleBodyTemplateChange} />
          <HelpBlock>The template that will be used to generate the email body.</HelpBlock>
        </FormGroup>
      </React.Fragment>
    );
  }
}

export default EmailNotificationForm;
