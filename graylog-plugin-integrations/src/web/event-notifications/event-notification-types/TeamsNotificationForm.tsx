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
      + '<b>--- [Event Definition] ---</b>\n'
      + '<table>\n'
      + '<tr><td><b>Title:</b></td><td>${event_definition_title}</td></tr>\n'
      + '<tr><td><b>Type:</b></td><td>${event_definition_type}</td></tr>\n'
      + '<table>\n'
      + '\n'
      + '<b>--- [Event] ---</b>\n'
      + '<table>\n'
      + '<tr><td><b>Timestamp:</b></td><td>${event.timestamp}</td></tr>\n'
      + '<tr><td><b>Message:</b></td><td>${event.message}</td></tr>\n'
      + '<tr><td><b>Source:</b></td><td>${event.source}</td></tr>\n'
      + '<tr><td><b>Key:</b></td><td>${event.key}</td></tr>\n'
      + '<tr><td><b>Priority:</b></td><td>${event.priority}</td></tr>\n'
      + '<tr><td><b>Alert:</b></td><td>${event.alert}</td></tr>\n'
      + '<tr><td><b>Timestamp Processing:</b></td><td>${event.timestamp}</td></tr>\n'
      + '<tr><td><b>Timerange Start:</b></td><td>${event.timerange_start}</td></tr>\n'
      + '<tr><td><b>Timerange End:</b></td><td>${event.timerange_end}</td></tr>\n'
      + '<table>\n'
      + '\n'
      + '<b>Event Fields:</b>\n'
      + '<table>\n'
      + '${foreach event.fields field}\n'
      + '<tr><td><b>${field.key}:</b></td><td>${field.value}</td></tr>\n'
      + '${end}\n'
      + '</table>\n'
      + '\n'
      + '${if backlog}\n'
      + '<b>--- [Backlog] ---</b>\n'
      + '${foreach backlog message}\n'
      + '<p><code>${message.timestamp}  ::  ${message.source}  ::  ${message.message}</code></p>\n'
      + '${end}${end}',
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
