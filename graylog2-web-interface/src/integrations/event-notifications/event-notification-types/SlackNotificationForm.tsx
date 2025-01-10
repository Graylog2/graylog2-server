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
import cloneDeep from 'lodash/cloneDeep';
import get from 'lodash/get';
import camelCase from 'lodash/camelCase';

import { Col, Input, Button, ControlLabel, FormControl, FormGroup, HelpBlock, InputGroup, Row } from 'components/bootstrap';
import { ColorPickerPopover, TimezoneSelect } from 'components/common';
import ColorLabel from 'components/sidecars/common/ColorLabel';
import DocumentationLink from 'components/support/DocumentationLink';
import { getValueFromInput } from 'util/FormsUtils';
import type { SlackConfigType, SlackValidationType } from 'integrations/event-notifications/types';

type Props = {
  config: SlackConfigType,
  validation: SlackValidationType,
  onChange: any,
};

class SlackNotificationForm extends React.Component<Props, any> {
  static defaultConfig = {
    color: '#FF0000',
    webhook_url: '',
    channel: '#channel',
    /* eslint-disable no-template-curly-in-string */
    custom_message: ''
                      + '--- [Event Definition] ---------------------------\n'
                      + 'Title:       ${event_definition_title}\n'
                      + 'Type:        ${event_definition_type}\n'
                      + '--- [Event] --------------------------------------\n'
                      + 'Alert Replay:         ${http_external_uri}alerts/${event.id}/replay-search\n'
                      + 'Timestamp:            ${event.timestamp}\n'
                      + 'Message:              ${event.message}\n'
                      + 'Source:               ${event.source}\n'
                      + 'Key:                  ${event.key}\n'
                      + 'Priority:             ${event.priority}\n'
                      + 'Alert:                ${event.alert}\n'
                      + 'Timestamp Processing: ${event.timestamp}\n'
                      + 'Timerange Start:      ${event.timerange_start}\n'
                      + 'Timerange End:        ${event.timerange_end}\n'
                      + 'Event Fields:\n'
                      + '${foreach event.fields field}\n'
                      + '${field.key}: ${field.value}\n'
                      + '${end}\n'
                      + '${if backlog}\n'
                      + '--- [Backlog] ------------------------------------\n'
                      + 'Last messages accounting for this alert:\n'
                      + '${foreach backlog message}\n'
                      + '${message.timestamp}  ::  ${message.source}  ::  ${message.message}\n'
                      + '${message.message}\n'
                      + '${end}'
                      + '${end}\n',
    /* eslint-enable no-template-curly-in-string */
    user_name: 'Graylog',
    notify_channel: false,
    link_names: false,
    icon_url: '',
    icon_emoji: '',
    backlog_size: 0,
    time_zone: 'UTC',
  };

  constructor(props) {
    super(props);

    const defaultBacklogSize = props.config.backlog_size;

    this.state = {
      isBacklogSizeEnabled: defaultBacklogSize > 0,
      backlogSize: defaultBacklogSize,
    };
  }

  handleBacklogSizeChange = (event) => {
    const { name } = event.target;
    const value = getValueFromInput(event.target);

    this.setState({ [camelCase(name)]: value });
    this.propagateChange(name, getValueFromInput(event.target));
  };

  toggleBacklogSize = () => {
    const { isBacklogSizeEnabled, backlogSize } = this.state;

    this.setState({ isBacklogSizeEnabled: !isBacklogSizeEnabled });
    this.propagateChange('backlog_size', (isBacklogSizeEnabled ? 0 : backlogSize));
  };

  propagateChange = (key, value) => {
    const { config, onChange } = this.props;
    const nextConfig = cloneDeep(config);
    nextConfig[key] = value;
    onChange(nextConfig);
  };

  handleColorChange = (color, _, hidePopover) => {
    hidePopover();
    this.propagateChange('color', color);
  };

  handleChange = (event) => {
    const { name } = event.target;
    this.propagateChange(name, getValueFromInput(event.target));
  };

  handleTimeZoneChange = (nextValue) => {
    this.propagateChange('time_zone', nextValue);
  };

  render() {
    const { config, validation } = this.props;
    const { isBacklogSizeEnabled, backlogSize } = this.state;
    const element = <p>Custom message to be appended below the alert title. See <DocumentationLink text="docs" page="alerts#notifications" /> for more details.</p>;

    return (
      <>

        <FormGroup controlId="color">
          <ControlLabel>Configuration color</ControlLabel>
          <div>
            <ColorLabel color={config.color} />
            <div style={{ display: 'inline-block', marginLeft: 15 }}>
              <ColorPickerPopover id="color"
                                  color={config.color || '#f06292'}
                                  placement="right"
                                  triggerNode={<Button bsSize="xsmall">Change color</Button>}
                                  onChange={this.handleColorChange} />
            </div>
          </div>
          <HelpBlock>Choose a color to use for this configuration.</HelpBlock>
        </FormGroup>
        <Input id="notification-webhookUrl"
               name="webhook_url"
               label="Webhook URL"
               type="text"
               bsStyle={validation.errors.webhook_url ? 'error' : null}
               help={get(validation, 'errors.webhook_url[0]', 'Slack "Incoming Webhook" URL')}
               value={config.webhook_url || ''}
               onChange={this.handleChange}
               required />
        <Input id="notification-channel"
               name="channel"
               label="Channel"
               type="text"
               bsStyle={validation.errors.channel ? 'error' : null}
               help={get(validation, 'errors.channel[0]', 'Name of Slack #channel or @user for a direct message')}
               value={config.channel || ''}
               onChange={this.handleChange}
               required />
        <Input id="notification-customMessage"
               name="custom_message"
               label="Custom Message (optional)"
               type="textarea"
               bsStyle={validation.errors.custom_message ? 'error' : null}
               help={get(validation, 'errors.custom_message[0]', element)}
               value={config.custom_message || ''}
               onChange={this.handleChange} />
        <Input id="notification-time-zone"
               help="Time zone used for timestamps in the notification body."
               label="Time zone for date/time values">
          <TimezoneSelect className="timezone-select"
                          name="time_zone"
                          value={config.time_zone}
                          onChange={this.handleTimeZoneChange}
                          clearable={false} />
        </Input>
        <FormGroup>
          <ControlLabel>Message Backlog Limit (optional)</ControlLabel>
          <InputGroup>
            <InputGroup.Addon>
              <input id="toggle_backlog_size"
                     type="checkbox"
                     checked={isBacklogSizeEnabled}
                     onChange={this.toggleBacklogSize} />
            </InputGroup.Addon>
            <FormControl type="number"
                         id="backlog_size"
                         name="backlog_size"
                         onChange={this.handleBacklogSizeChange}
                         value={backlogSize}
                         min="0"
                         disabled={!isBacklogSizeEnabled} />
          </InputGroup>
          <HelpBlock>Limit the number of backlog messages sent as part of the Slack notification.  If set to 0, no limit will be enforced.</HelpBlock>
        </FormGroup>

        <Input id="notification-userName"
               name="user_name"
               label="User Name (optional)"
               type="text"
               bsStyle={validation.errors.user_name ? 'error' : null}
               help={get(validation, 'errors.user_name[0]', 'User name of the sender in Slack')}
               value={config.user_name || ''}
               onChange={this.handleChange} />
        <Row>
          <Col md={4}>
            <Input id="include_title"
                   name="include_title"
                   label="Include Title"
                   bsStyle={validation.errors.include_title ? 'error' : null}
                   help={get(validation, 'errors.include_title[0]', 'Include the event definition title and description in the notification')}
                   type="checkbox"
                   checked={config.include_title}
                   onChange={this.handleChange} />
          </Col>
          <Col md={4}>
            <Input id="notification-notifyChannel"
                   name="notify_channel"
                   label="Notify Channel"
                   type="checkbox"
                   bsStyle={validation.errors.notify_channel ? 'error' : null}
                   help={get(validation, 'errors.notify_channel[0]', 'Notify all users in channel by adding @channel to the message')}
                   checked={config.notify_channel ?? false}
                   onChange={this.handleChange} />
          </Col>
          <Col md={4}>
            <Input id="notification-notifyHere"
                   name="notify_here"
                   label="Notify Here"
                   type="checkbox"
                   bsStyle={validation.errors.notify_here ? 'error' : null}
                   help={get(validation, 'errors.notify_here[0]', 'Notify active users in channel by adding @here to the message')}
                   checked={config.notify_here ?? false}
                   onChange={this.handleChange} />
          </Col>
        </Row>
        <Input id="notification-linkNames"
               name="link_names"
               label="Link Names"
               type="checkbox"
               bsStyle={validation.errors.link_names ? 'error' : null}
               help={get(validation, 'errors.link_names[0]', 'Find and link channel names and user names')}
               checked={!!config.link_names}
               onChange={this.handleChange} />
        <Input id="notification-iconUrl"
               name="icon_url"
               label="Icon URL (optional)"
               type="text"
               bsStyle={validation.errors.icon_url ? 'error' : null}
               help={get(validation, 'errors.icon_url[0]', 'Image to use as the icon for this message')}
               value={config.icon_url || ''}
               onChange={this.handleChange} />
        <Input id="notification-iconEmoji"
               name="icon_emoji"
               label="Icon Emoji (optional)"
               type="text"
               bsStyle={validation.errors.icon_emoji ? 'error' : null}
               help={get(validation, 'errors.icon_emoji[0]', 'Emoji to use as the icon for this message (overrides Icon URL)')}
               value={config.icon_emoji || ''}
               onChange={this.handleChange} />
      </>
    );
  }
}

export default SlackNotificationForm;
