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
import { Input, Button, ControlLabel, FormControl, FormGroup, HelpBlock, InputGroup } from 'components/bootstrap';
import { ColorPickerPopover, TimezoneSelect } from 'components/common';
import ColorLabel from 'components/sidecars/common/ColorLabel';
import DocsHelper from 'util/DocsHelper';
import DocumentationLink from 'components/support/DocumentationLink';

import type { ConfigType, ValidationType } from '../types';

type TeamsNotificationFormType = {
  config: ConfigType;
  validation: ValidationType;
  onChange: any;
};

export const defaultConfig: ConfigType = {
  color: '#FF0000',
  webhook_url: '',
  /* eslint-disable no-template-curly-in-string */
  custom_message:
    '' +
    '<b>--- [Event Definition] ---</b>\n' +
    '<table>\n' +
    '<tr><td><b>Title:</b></td><td>${event_definition_title}</td></tr>\n' +
    '<tr><td><b>Type:</b></td><td>${event_definition_type}</td></tr>\n' +
    '<table>\n' +
    '\n' +
    '<b>--- [Event] ---</b>\n' +
    '<table>\n' +
    '<tr><td><b>Alert Replay:</b></td><td>${http_external_uri}alerts/${event.id}/replay-search</td></tr>\n' +
    '<tr><td><b>Timestamp:</b></td><td>${event.timestamp}</td></tr>\n' +
    '<tr><td><b>Message:</b></td><td>${event.message}</td></tr>\n' +
    '<tr><td><b>Source:</b></td><td>${event.source}</td></tr>\n' +
    '<tr><td><b>Key:</b></td><td>${event.key}</td></tr>\n' +
    '<tr><td><b>Priority:</b></td><td>${event.priority}</td></tr>\n' +
    '<tr><td><b>Alert:</b></td><td>${event.alert}</td></tr>\n' +
    '<tr><td><b>Timestamp Processing:</b></td><td>${event.timestamp}</td></tr>\n' +
    '<tr><td><b>Timerange Start:</b></td><td>${event.timerange_start}</td></tr>\n' +
    '<tr><td><b>Timerange End:</b></td><td>${event.timerange_end}</td></tr>\n' +
    '<table>\n' +
    '\n' +
    '<b>Event Fields:</b>\n' +
    '<table>\n' +
    '${foreach event.fields field}\n' +
    '<tr><td><b>${field.key}:</b></td><td>${field.value}</td></tr>\n' +
    '${end}\n' +
    '</table>\n' +
    '\n' +
    '${if backlog}\n' +
    '<b>--- [Backlog] ---</b>\n' +
    '${foreach backlog message}\n' +
    '<p><code>${message.timestamp}  ::  ${message.source}  ::  ${message.message}</code></p>\n' +
    '${end}${end}',
  /* eslint-enable no-template-curly-in-string */
  icon_url: '',
  backlog_size: 0,
  time_zone: 'UTC',
};

const TeamsNotificationForm = ({ config, validation, onChange }: TeamsNotificationFormType) => {
  const defaultBacklogSize = config.backlog_size;
  const [isBacklogSizeEnabled, setIsBacklogSizeEnabled] = useState(defaultBacklogSize > 0);
  const [backlogSize, setBacklogSize] = useState(defaultBacklogSize);

  const propagateChange = (key: string, value: any) => {
    const nextConfig = cloneDeep(config);
    nextConfig[key] = value;
    onChange(nextConfig);
  };

  const handleBacklogSizeChange = (event: any) => {
    const { name } = event.target;
    const value = Number(getValueFromInput(event.target));

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

  const handleColorChange: (color: string, _: any, hidePopover: any) => void = (color, _, hidePopover) => {
    hidePopover();
    propagateChange('color', color);
  };

  const handleTimeZoneChange = (nextValue) => {
    propagateChange('time_zone', nextValue);
  };

  const handleChange = (event: any) => {
    const { name } = event.target;
    propagateChange(name, getValueFromInput(event.target));
  };

  const element = (
    <p>
      Custom message to be appended below the alert title. See{' '}
      <DocumentationLink page={DocsHelper.PAGES.ALERTS} text="docs " /> for more details.
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
        help={validation?.errors?.webhook_url?.[0] ?? 'Teams "Incoming Webhook" URL'}
        value={config.webhook_url || ''}
        onChange={handleChange}
        required
      />
      <Input
        id="notification-customMessage"
        name="custom_message"
        label="Custom Message (optional)"
        type="textarea"
        bsStyle={validation.errors.custom_message ? 'error' : null}
        help={validation?.errors?.custom_message?.[0] ?? element}
        value={config.custom_message || ''}
        onChange={handleChange}
      />

      <FormGroup>
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
          Limit the number of backlog messages sent as part of the Microsoft Teams notification. If set to 0, no limit
          will be enforced.
        </HelpBlock>
      </FormGroup>

      <Input
        id="notification-iconUrl"
        name="icon_url"
        label="Icon URL (optional)"
        type="text"
        bsStyle={validation.errors.icon_url ? 'error' : null}
        help={validation?.errors?.icon_url?.[0] ?? 'Image to use as the icon for this message'}
        value={config.icon_url || ''}
        onChange={handleChange}
      />
    </>
  );
};

export default TeamsNotificationForm;
