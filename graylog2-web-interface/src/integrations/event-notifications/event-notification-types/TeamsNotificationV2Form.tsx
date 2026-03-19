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
import { Input, ControlLabel, FormControl, FormGroup, HelpBlock, InputGroup } from 'components/bootstrap';
import { TimezoneSelect, URLAllowListInput, SourceCodeEditor } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import DocumentationLink from 'components/support/DocumentationLink';

import type { ValidationType, ConfigV2Type } from '../types';

type TeamsNotificationFormV2Type = {
  config: ConfigV2Type;
  validation: ValidationType;
  onChange: any;
};

export const defaultConfig: ConfigV2Type = {
  webhook_url: '',
  /* eslint-disable no-template-curly-in-string */
  adaptive_card:
    '{\n' +
    '  "type": "message",\n' +
    '  "attachments": [\n' +
    '    {\n' +
    '      "contentType": "application/vnd.microsoft.card.adaptive",\n' +
    '      "content": {\n' +
    '        "type": "AdaptiveCard",\n' +
    '        "version": "1.5",\n' +
    '        "msTeams": { "width": "full" },\n' +
    '        "body": [\n' +
    '          {\n' +
    '            "type": "TextBlock",\n' +
    '            "size": "Large",\n' +
    '            "weight": "Bolder",\n' +
    '            "text": "${event_definition_title} triggered",\n' +
    '            "style": "heading",\n' +
    '            "fontType": "Default"\n' +
    '          },\n' +
    '          {\n' +
    '            "type": "TextBlock",\n' +
    '            "text": "${event_definition_description}",\n' +
    '            "wrap": true\n' +
    '          },\n' +
    '          {\n' +
    '            "type": "TextBlock",\n' +
    '            "text": "Event Details",\n' +
    '            "wrap": true\n' +
    '          },\n' +
    '          {\n' +
    '            "type": "FactSet",\n' +
    '            "facts": [\n' +
    '              {\n' +
    '                "title": "Type",\n' +
    '                "value": "${event_definition_type}"\n' +
    '              },\n' +
    '              {\n' +
    '                "title": "Timestamp",\n' +
    '                "value": "${event.timestamp_processing}"\n' +
    '              },\n' +
    '              {\n' +
    '                "title": "Message",\n' +
    '                "value": "${event.message}"\n' +
    '              },\n' +
    '              {\n' +
    '                "title": "Source",\n' +
    '                "value": "${event.source}"\n' +
    '              },\n' +
    '              {\n' +
    '                "title": "Key",\n' +
    '                "value": "${event.key}"\n' +
    '              },\n' +
    '              {\n' +
    '                "title": "Priority",\n' +
    '                "value": "${event.priority}"\n' +
    '              },\n' +
    '              {\n' +
    '                "title": "Alert",\n' +
    '                "value": "${event.alert}"\n' +
    '              },\n' +
    '              {\n' +
    '                "title": "Timerange Start",\n' +
    '                "value": "${event.timerange_start}"\n' +
    '              },\n' +
    '              {\n' +
    '                "title": "Timerange End",\n' +
    '                "value": "${event.timerange_end}"\n' +
    '              }\n' +
    '            ]\n' +
    '          }${if event.fields},\n' +
    '          {\n' +
    '            "type": "TextBlock",\n' +
    '            "text": "Event Fields",\n' +
    '            "weight": "bolder",\n' +
    '            "size": "medium"\n' +
    '          },\n' +
    '          {\n' +
    '            "type": "FactSet",\n' +
    '            "facts": [${foreach event.fields field}\n' +
    '              { "title": "${field.key}", "value": "${field.value}" }${if last_field}${else},${end}${end}\n' +
    '            ]\n' +
    '          }${end}${if backlog},\n' +
    '          {\n' +
    '            "type": "TextBlock",\n' +
    '            "text": "Backlog",\n' +
    '            "weight": "bolder",\n' +
    '            "size": "medium"\n' +
    '          },\n' +
    '          {\n' +
    '            "type": "FactSet",\n' +
    '            "facts": [${foreach backlog message}\n' +
    '              { "title": "Message", "value": "${message.message}" }${if last_message}${else},${end}${end}\n' +
    '            ]\n' +
    '          }${end}\n' +
    '        ],\n' +
    '        "actions": [{\n' +
    '          "type": "Action.OpenUrl",\n' +
    '          "title": "Replay Search",\n' +
    '          "url": "${http_external_uri}alerts/${event.id}/replay-search"\n' +
    '        }],\n' +
    '        "$schema": "http://adaptivecards.io/schemas/adaptive-card.json",\n' +
    '        "rtl": false\n' +
    '      }\n' +
    '    }\n' +
    '  ]\n' +
    '}',
  /* eslint-enable no-template-curly-in-string */
  backlog_size: 0,
  time_zone: 'UTC',
};

const TeamsNotificationV2Form = ({ config, validation, onChange }: TeamsNotificationFormV2Type) => {
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

  const handleTimeZoneChange = (nextValue) => {
    propagateChange('time_zone', nextValue);
  };

  const handleWebhookUrlChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    propagateChange('webhook_url', getValueFromInput(event.target));
  };

  const handleAdaptiveCardChange = (nextValue: string) => {
    propagateChange('adaptive_card', nextValue);
  };

  const element = (
    <p>
      Adaptive Card to post to Teams. See <DocumentationLink page={DocsHelper.PAGES.ALERTS} text="docs " /> for more
      details.
    </p>
  );

  return (
    <>
      <URLAllowListInput
        label="Webhook URL"
        onChange={handleWebhookUrlChange}
        validationState={validation.errors.webhook_url ? 'error' : null}
        validationMessage={validation?.errors?.webhook_url?.[0] ?? 'Teams "Incoming Webhook" URL'}
        url={config.webhook_url || ''}
        autofocus={false}
      />
      <FormGroup>
        <ControlLabel>Adaptive Card Template</ControlLabel>
        <SourceCodeEditor
          id="notification-adaptiveCard"
          mode="text"
          theme="light"
          value={config.adaptive_card || ''}
          wrapEnabled
          onChange={handleAdaptiveCardChange}
        />
        <HelpBlock>{validation?.errors?.adaptive_card?.[0] ?? element}</HelpBlock>
      </FormGroup>
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
    </>
  );
};

export default TeamsNotificationV2Form;
