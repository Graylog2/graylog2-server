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

import { getValueFromInput } from 'util/FormsUtils';
import { Input, Button, ControlLabel, FormControl, FormGroup, HelpBlock, InputGroup } from 'components/bootstrap';
import { ColorPickerPopover } from 'components/common';
import ColorLabel from 'components/sidecars/common/ColorLabel';

import type { ConfigType, ValidationType } from '../types';

type TeamsNotificationFormType = {
      config: ConfigType,
      validation: ValidationType
      onChange: any
}

class TeamsNotificationForm extends React.Component<TeamsNotificationFormType, any> {
  static defaultConfig = {
    color: '#FF0000',
    webhook_url: '',
    /* eslint-disable no-template-curly-in-string */
    custom_message: ''
                      + '--- [Event Definition] ----\n'
                      + 'Title:       ${event_definition_title}\n'
                      + 'Type:        ${event_definition_type}\n'
                      + '--- [Event] ----\n'
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
                      + '--- [Backlog] ----------\n'
                      + 'Last messages accounting for this alert:\n'
                      + '${foreach backlog message}\n'
                      + '${message.timestamp}  ::  ${message.source}  ::  ${message.message}\n'
                      + '${message.message}\n'
                      + '${end}'
                      + '${end}\n',
    /* eslint-enable no-template-curly-in-string */
    icon_url: '',
    backlog_size: 0,

  };

  constructor(props: TeamsNotificationFormType | Readonly<TeamsNotificationFormType>) {
    super(props);

    const defaultBacklogSize = props.config.backlog_size;

    this.state = {
      isBacklogSizeEnabled: defaultBacklogSize > 0,
      backlogSize: defaultBacklogSize,
    };
  }

  handleBacklogSizeChange = (event: { target: { name: string; }; }) => {
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

  propagateChange = (key: string, value: any) => {
    const { config, onChange } = this.props;
    const nextConfig = cloneDeep(config);
    nextConfig[key] = value;
    onChange(nextConfig);
  };

  handleColorChange: (color: string, _: any, hidePopover: any) => void = (color, _, hidePopover) => {
    hidePopover();
    this.propagateChange('color', color);
  };

  handleChange = (event: { target: { name: any; }; }) => {
    const { name } = event.target;
    this.propagateChange(name, getValueFromInput(event.target));
  };

  render() {
    const { config, validation } = this.props;
    const { isBacklogSizeEnabled, backlogSize } = this.state;
    const url = 'https://docs.graylog.org/docs/alerts#notifications';
    const element = <p>Custom message to be appended below the alert title. See <a href={url} rel="noopener noreferrer" target="_blank">docs </a>for more details.</p>;

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
               help={get(validation, 'errors.webhook_url[0]', 'Teams "Incoming Webhook" URL')}
               value={config.webhook_url || ''}
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
          <HelpBlock>Limit the number of backlog messages sent as part of the Microsoft Teams notification.  If set to 0, no limit will be enforced.</HelpBlock>
        </FormGroup>

        <Input id="notification-iconUrl"
               name="icon_url"
               label="Icon URL (optional)"
               type="text"
               bsStyle={validation.errors.icon_url ? 'error' : null}
               help={get(validation, 'errors.icon_url[0]', 'Image to use as the icon for this message')}
               value={config.icon_url || ''}
               onChange={this.handleChange} />

      </>
    );
  }
}

export default TeamsNotificationForm;
