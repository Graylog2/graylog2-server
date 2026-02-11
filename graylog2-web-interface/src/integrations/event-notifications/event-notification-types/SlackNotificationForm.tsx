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
import React, { useState } from 'react';
import cloneDeep from 'lodash/cloneDeep';

import { getValueFromInput } from 'util/FormsUtils';
import type { SlackConfigType, SlackValidationType } from 'integrations/event-notifications/types';
import {
  Col,
  Input,
  Button,
  ControlLabel,
  FormControl,
  FormGroup,
  HelpBlock,
  InputGroup,
  Row,
} from 'components/bootstrap';
import { ColorPickerPopover, TimezoneSelect } from 'components/common';
import ColorLabel from 'components/sidecars/common/ColorLabel';
import DocumentationLink from 'components/support/DocumentationLink';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';

type EventProcedureCheckboxProps = {
  checked: boolean;
  onChange: (e: React.ChangeEvent<any>) => void;
  validation: SlackValidationType;
};

function EventProcedureCheckbox({ checked, onChange, validation }: EventProcedureCheckboxProps) {
  const {
    data: { valid: validSecurityLicense },
  } = usePluggableLicenseCheck('/license/security');

  if (!validSecurityLicense) return null;

  return (
    <Input
      id="include_event_procedure"
      name="include_event_procedure"
      label="Include Event Procedure"
      help={
        validation?.errors?.include_event_procedure?.[0] ||
        "Append a formatted version of the event definition's event procedure to the end of the notification body."
      }
      type="checkbox"
      checked={checked}
      onChange={onChange}
    />
  );
}

export const defaultConfig: SlackConfigType = {
  color: '#FF0000',
  webhook_url: '',
  channel: '#channel',
  /* eslint-disable no-template-curly-in-string */
  custom_message:
    '' +
    '--- [Event Definition] ---------------------------\n' +
    'Title:       ${event_definition_title}\n' +
    'Type:        ${event_definition_type}\n' +
    '--- [Event] --------------------------------------\n' +
    'Alert Replay:         ${http_external_uri}alerts/${event.id}/replay-search\n' +
    'Timestamp:            ${event.timestamp}\n' +
    'Message:              ${event.message}\n' +
    'Source:               ${event.source}\n' +
    'Key:                  ${event.key}\n' +
    'Priority:             ${event.priority}\n' +
    'Alert:                ${event.alert}\n' +
    'Timestamp Processing: ${event.timestamp}\n' +
    'Timerange Start:      ${event.timerange_start}\n' +
    'Timerange End:        ${event.timerange_end}\n' +
    'Event Fields:\n' +
    '${foreach event.fields field}\n' +
    '${field.key}: ${field.value}\n' +
    '${end}\n' +
    '${if backlog}\n' +
    '--- [Backlog] ------------------------------------\n' +
    'Last messages accounting for this alert:\n' +
    '${foreach backlog message}\n' +
    '${message.timestamp}  ::  ${message.source}  ::  ${message.message}\n' +
    '${message.message}\n' +
    '${end}' +
    '${end}\n',
  /* eslint-enable no-template-curly-in-string */
  user_name: 'Username',
  notify_channel: false,
  link_names: '',
  icon_url: '',
  icon_emoji: '',
  backlog_size: 0,
  time_zone: 'UTC',
  include_title: undefined,
  notify_here: undefined,
  include_event_procedure: false,
};

type Props = {
  config: SlackConfigType;
  validation: SlackValidationType;
  onChange: any;
};

const SlackNotificationForm = ({ config, validation, onChange }: Props) => {
  const defaultBacklogSize = config.backlog_size;
  const [isBacklogSizeEnabled, setIsBacklogSizeEnabled] = useState(defaultBacklogSize > 0);
  const [backlogSize, setBacklogSize] = useState(defaultBacklogSize);

  const propagateChange = (key, value) => {
    const nextConfig = cloneDeep(config);
    nextConfig[key] = value;
    onChange(nextConfig);
  };

  const handleBacklogSizeChange = (event) => {
    const { name } = event.target;
    const value = getValueFromInput(event.target);

    setBacklogSize(value);
    propagateChange(name, value);
  };

  const toggleBacklogSize = () => {
    setIsBacklogSizeEnabled((prevIsBacklogSizeEnabled) => {
      const nextIsBacklogSizeEnabled = !prevIsBacklogSizeEnabled;
      propagateChange('backlog_size', prevIsBacklogSizeEnabled ? 0 : backlogSize);

      return nextIsBacklogSizeEnabled;
    });
  };

  const handleColorChange = (color, _, hidePopover) => {
    hidePopover();
    propagateChange('color', color);
  };

  const handleChange = (event) => {
    const { name } = event.target;
    propagateChange(name, getValueFromInput(event.target));
  };

  const handleTimeZoneChange = (nextValue) => {
    propagateChange('time_zone', nextValue);
  };

  const element = (
    <p>
      Custom message to be appended below the alert title. See{' '}
      <DocumentationLink text="docs" page="alerts#notifications" /> for more details.
    </p>
  );

  return (
    <>
      <FormGroup controlId="color">
        <ControlLabel>Configuration color</ControlLabel>
        <div>
          <ColorLabel color={config.color} />
          <div style={{ display: 'inline-block', marginLeft: 15 }}>
            <ColorPickerPopover
              id="color"
              color={config.color || '#f06292'}
              placement="right"
              triggerNode={<Button bsSize="xsmall">Change color</Button>}
              onChange={handleColorChange}
            />
          </div>
        </div>
        <HelpBlock>Choose a color to use for this configuration.</HelpBlock>
      </FormGroup>
      <Input
        id="notification-webhookUrl"
        name="webhook_url"
        label="Webhook URL"
        type="text"
        bsStyle={validation.errors.webhook_url ? 'error' : null}
        help={validation?.errors?.webhook_url?.[0] || 'Slack "Incoming Webhook" URL'}
        value={config.webhook_url || ''}
        onChange={handleChange}
        required
      />
      <Input
        id="notification-channel"
        name="channel"
        label="Channel"
        type="text"
        bsStyle={validation.errors.channel ? 'error' : null}
        help={validation?.errors?.channel?.[0] || 'Name of Slack #channel or @user for a direct message'}
        value={config.channel || ''}
        onChange={handleChange}
        required
      />
      <Input
        id="notification-customMessage"
        name="custom_message"
        label="Custom Message (optional)"
        type="textarea"
        bsStyle={validation.errors.custom_message ? 'error' : null}
        help={validation?.errors?.custom_message?.[0] || element}
        value={config.custom_message || ''}
        onChange={handleChange}
      />
      <EventProcedureCheckbox
        checked={config.include_event_procedure}
        onChange={handleChange}
        validation={validation}
      />
      <Input
        id="notification-time-zone"
        help="Time zone used for timestamps in the notification body."
        label="Time zone for date/time values">
        <TimezoneSelect
          className="timezone-select"
          name="time_zone"
          value={config.time_zone}
          onChange={handleTimeZoneChange}
          clearable={false}
        />
      </Input>
      <FormGroup>
        <ControlLabel>Message Backlog Limit (optional)</ControlLabel>
        <InputGroup>
          <InputGroup.Addon>
            <input
              id="toggle_backlog_size"
              type="checkbox"
              checked={isBacklogSizeEnabled}
              onChange={toggleBacklogSize}
            />
          </InputGroup.Addon>
          <FormControl
            type="number"
            id="backlog_size"
            name="backlog_size"
            onChange={handleBacklogSizeChange}
            value={backlogSize}
            min="0"
            disabled={!isBacklogSizeEnabled}
          />
        </InputGroup>
        <HelpBlock>
          Limit the number of backlog messages sent as part of the Slack notification. If set to 0, no limit will be
          enforced.
        </HelpBlock>
      </FormGroup>

      <Input
        id="notification-userName"
        name="user_name"
        label="User Name (optional)"
        type="text"
        bsStyle={validation.errors.user_name ? 'error' : null}
        help={validation?.errors?.user_name?.[0] || 'User name of the sender in Slack'}
        value={config.user_name || ''}
        onChange={handleChange}
      />
      <Row>
        <Col md={4}>
          <Input
            id="include_title"
            name="include_title"
            label="Include Title"
            bsStyle={validation.errors.include_title ? 'error' : null}
            help={
              validation?.errors?.include_title?.[0] ||
              'Include the event definition title and description in the notification'
            }
            type="checkbox"
            checked={config.include_title}
            onChange={handleChange}
          />
        </Col>
        <Col md={4}>
          <Input
            id="notification-notifyChannel"
            name="notify_channel"
            label="Notify Channel"
            type="checkbox"
            bsStyle={validation.errors.notify_channel ? 'error' : null}
            help={
              validation?.errors?.notify_channel?.[0] || 'Notify all users in channel by adding @channel to the message'
            }
            checked={config.notify_channel ?? false}
            onChange={handleChange}
          />
        </Col>
        <Col md={4}>
          <Input
            id="notification-notifyHere"
            name="notify_here"
            label="Notify Here"
            type="checkbox"
            bsStyle={validation.errors.notify_here ? 'error' : null}
            help={
              validation?.errors?.notify_here?.[0] || 'Notify active users in channel by adding @here to the message'
            }
            checked={config.notify_here ?? false}
            onChange={handleChange}
          />
        </Col>
      </Row>
      <Input
        id="notification-linkNames"
        name="link_names"
        label="Link Names"
        type="checkbox"
        bsStyle={validation.errors.link_names ? 'error' : null}
        help={validation?.errors?.link_names?.[0] || 'Find and link channel names and user names'}
        checked={!!config.link_names}
        onChange={handleChange}
      />
      <Input
        id="notification-iconUrl"
        name="icon_url"
        label="Icon URL (optional)"
        type="text"
        bsStyle={validation.errors.icon_url ? 'error' : null}
        help={validation?.errors?.icon_url?.[0] || 'Image to use as the icon for this message'}
        value={config.icon_url || ''}
        onChange={handleChange}
      />
      <Input
        id="notification-iconEmoji"
        name="icon_emoji"
        label="Icon Emoji (optional)"
        type="text"
        bsStyle={validation.errors.icon_emoji ? 'error' : null}
        help={validation?.errors?.icon_emoji?.[0] || 'Emoji to use as the icon for this message (overrides Icon URL)'}
        value={config.icon_emoji || ''}
        onChange={handleChange}
      />
    </>
  );
};

export default SlackNotificationForm;
