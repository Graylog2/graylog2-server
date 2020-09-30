import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Input } from 'components/bootstrap';
import FormsUtils from 'util/FormsUtils';

class SlackNotificationForm extends React.Component {
    static propTypes = {
      config: PropTypes.object.isRequired,
      validation: PropTypes.object.isRequired,
      onChange: PropTypes.func.isRequired,
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

    render() {
      const { config, validation } = this.props;

      return (
        <>
          <Input id="notification-color"
                 name="color"
                 label="Custom Message Color"
                 type="text"
                 bsStyle={validation.errors.color ? 'error' : null}
                 help={lodash.get(validation, 'errors.color[0]', 'Color to use for Slack message')}
                 value={config.color || ''}
                 onChange={this.handleChange}
                 required />
          <Input id="notification-webhookUrl"
                 name="webhook_url"
                 label="Webhook URL"
                 type="text"
                 bsStyle={validation.errors.webhook_url ? 'error' : null}
                 help={lodash.get(validation, 'errors.webhook_url[0]', 'Slack "Incoming Webhook" URL')}
                 value={config.webhook_url || ''}
                 onChange={this.handleChange}
                 required />
          <Input id="notification-channel"
                 name="channel"
                 label="Channel"
                 type="text"
                 bsStyle={validation.errors.channel ? 'error' : null}
                 help={lodash.get(validation, 'errors.channel[0]', 'Name of Slack #channel or @user for a direct message')}
                 value={config.channel || ''}
                 onChange={this.handleChange}
                 required />
          <Input id="notification-customMessage"
                 name="custom_message"
                 label="Custom Message (optional)"
                 type="textarea"
                 bsStyle={validation.errors.custom_message ? 'error' : null}
                 help={lodash.get(validation, 'errors.custom_message[0]', 'Custom message to be appended below the alert title. The following properties are available for template building: "event_definition", "event", "backlog", "backlog_size", "streams", "graylog_url". See http://docs.graylog.org/en/3.1/pages/streams/alerts.html for more details.')}
                 value={config.custom_message || ''}
                 onChange={this.handleChange} />
          <Input id="notification-backlogItemMessage"
                 name="backlog_item_message"
                 label="Backlog Item Message (optional)"
                 type="textarea"
                 bsStyle={validation.errors.backlog_item_message ? 'error' : null}
                 help={lodash.get(validation, 'errors.backlog_item_message[0]', 'Template that is added as attachment to the slack message for each backlog item. The following properties are available for template building: "event_definition", "event", "backlog_item", "streams", "graylog_url". See http://docs.graylog.org/en/3.1/pages/streams/alerts.html for more details. Slack recommends to have no more than 20 attachments and throws an error when attempting to include more than 100. So don\'t use a too high backlog items number. Also consider that other parts of the slack notification may use attachments!')}
                 value={config.backlog_item_message || ''}
                 onChange={this.handleChange} />
          <Input id="notification-userName"
                 name="user_name"
                 label="User Name (optional)"
                 type="text"
                 bsStyle={validation.errors.user_name ? 'error' : null}
                 help={lodash.get(validation, 'errors.user_name[0]', 'User name of the sender in Slack')}
                 value={config.user_name || ''}
                 onChange={this.handleChange} />
          <Input id="notification-notifyChannel"
                 name="notify_channel"
                 label="Notify Channel (optional)"
                 type="checkbox"
                 bsStyle={validation.errors.notify_channel ? 'error' : null}
                 help={lodash.get(validation, 'errors.notify_channel[0]', 'Notify all users in channel by adding @channel to the message')}
                 checked={config.notify_channel || ''}
                 onChange={this.handleChange} />
          <Input id="notification-linkNames"
                 name="link_names"
                 label="Link Names (optional)"
                 type="checkbox"
                 bsStyle={validation.errors.link_names ? 'error' : null}
                 help={lodash.get(validation, 'errors.link_names[0]', 'Find and link channel names and user names')}
                 checked={config.link_names || ''}
                 onChange={this.handleChange} />
          <Input id="notification-iconUrl"
                 name="icon_url"
                 label="Icon URL (optional)"
                 type="text"
                 bsStyle={validation.errors.icon_url ? 'error' : null}
                 help={lodash.get(validation, 'errors.icon_url[0]', 'Image to use as the icon for this message')}
                 value={config.icon_url || ''}
                 onChange={this.handleChange} />
          <Input id="notification-iconEmoji"
                 name="icon_emoji"
                 label="Icon Emoji (optional)"
                 type="text"
                 bsStyle={validation.errors.icon_emoji ? 'error' : null}
                 help={lodash.get(validation, 'errors.icon_emoji[0]', 'Emoji to use as the icon for this message (overrides Icon URL)')}
                 value={config.icon_emoji || ''}
                 onChange={this.handleChange} />
          <Input id="notification-graylogUrl"
                 name="graylog_url"
                 label="Graylog URL (optional)"
                 type="text"
                 bsStyle={validation.errors.graylog_url ? 'error' : null}
                 help={lodash.get(validation, 'errors.graylog_url[0]', 'URL to your Graylog web interface. Used to build links in alarm notification')}
                 value={config.graylog_url || ''}
                 onChange={this.handleChange} />
          <Input id="notification-proxy"
                 name="proxy"
                 label="Proxy (optional)"
                 type="text"
                 bsStyle={validation.errors.proxy ? 'error' : null}
                 help={lodash.get(validation, 'errors.proxy[0]', 'Please insert the proxy information in the follwoing format: <ProxyAddress>:<Port>')}
                 value={config.proxy || ''}
                 onChange={this.handleChange} />
        </>
      );
    }
}

export default SlackNotificationForm;
