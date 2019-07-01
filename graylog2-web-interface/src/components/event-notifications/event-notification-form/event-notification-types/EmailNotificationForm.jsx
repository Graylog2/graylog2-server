import React from 'react';
import PropTypes from 'prop-types';
import { ControlLabel, FormGroup, HelpBlock } from 'react-bootstrap';
import lodash from 'lodash';

import { MultiSelect } from 'components/common';

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
      user_recipients: [],
      email_recipients: [],
    };
    onChange(Object.assign({}, initialConfig, config));
  }

  handleRecipientsChange = (key) => {
    return (nextValue) => {
      const { config, onChange } = this.props;
      const nextConfig = lodash.cloneDeep(config);
      nextConfig[key] = nextValue === '' ? [] : nextValue.split(',');
      onChange(nextConfig);
    };
  };

  formatUsers = (users) => {
    return users.map(user => ({ label: `${user.username} (${user.full_name})`, value: user.username }));
  };

  render() {
    const { config, users } = this.props;

    return (
      <React.Fragment>
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
      </React.Fragment>
    );
  }
}

export default EmailNotificationForm;
