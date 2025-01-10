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
import camelCase from 'lodash/camelCase';
import cloneDeep from 'lodash/cloneDeep';
import defaultTo from 'lodash/defaultTo';
import max from 'lodash/max';
import moment from 'moment';
import styled, { css } from 'styled-components';

import { ControlLabel, FormControl, FormGroup, InputGroup } from 'components/bootstrap';
import { TimeUnitInput } from 'components/common';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import * as FormsUtils from 'util/FormsUtils';

const TIME_UNITS = ['HOURS', 'MINUTES', 'SECONDS'];

const Container = styled.div(({ theme }) => css`
  padding-top: ${theme.spacings.lg};
`);

type NotificationSettingsFormProps = {
  eventDefinition: any;
  defaults: any;
  onSettingsChange: (...args: any[]) => void;
};

class NotificationSettingsForm extends React.Component<NotificationSettingsFormProps, {
  [key: string]: any;
}> {
  constructor(props) {
    super(props);

    const { backlog_size: backlogSize, grace_period_ms: gracePeriodMs } = props.eventDefinition.notification_settings;

    const gracePeriod = extractDurationAndUnit(gracePeriodMs, TIME_UNITS);
    const defaultBacklogSize = props.defaults.default_backlog_size;
    const effectiveBacklogSize = defaultTo(backlogSize, defaultBacklogSize);

    this.state = {
      gracePeriodDuration: gracePeriod.duration,
      gracePeriodUnit: gracePeriod.unit,
      isBacklogSizeEnabled: (backlogSize === null ? false : (effectiveBacklogSize > 0)),
      backlogSize: effectiveBacklogSize,
    };
  }

  propagateChanges = (key, value) => {
    const { eventDefinition, onSettingsChange } = this.props;
    const nextNotificationSettings = cloneDeep(eventDefinition.notification_settings);

    nextNotificationSettings[key] = value;
    onSettingsChange('notification_settings', nextNotificationSettings);
  };

  handleGracePeriodChange = (nextValue, nextUnit, enabled) => {
    const durationInMs = enabled ? moment.duration(max([nextValue, 0]), nextUnit).asMilliseconds() : 0;

    this.propagateChanges('grace_period_ms', durationInMs);
    this.setState({ gracePeriodDuration: nextValue, gracePeriodUnit: nextUnit });
  };

  handleBacklogSizeChange = (event) => {
    const { name } = event.target;
    const value = event.target.value === '' ? '' : FormsUtils.getValueFromInput(event.target);

    this.setState({ [camelCase(name)]: value });
    this.propagateChanges(name, max([Number(value), 0]));
  };

  toggleBacklogSize = () => {
    const { isBacklogSizeEnabled, backlogSize } = this.state;

    this.setState({ isBacklogSizeEnabled: !isBacklogSizeEnabled });
    this.propagateChanges('backlog_size', (isBacklogSizeEnabled ? 0 : backlogSize));
  };

  render() {
    const { eventDefinition } = this.props;
    const { gracePeriodDuration, gracePeriodUnit, isBacklogSizeEnabled, backlogSize } = this.state;

    if (eventDefinition.notifications.length === 0) {
      return null;
    }

    return (
      <Container>
        <FormGroup controlId="grace-period">
          <TimeUnitInput label="Grace Period"
                         update={this.handleGracePeriodChange}
                         defaultEnabled={gracePeriodDuration !== 0}
                         value={gracePeriodDuration}
                         unit={gracePeriodUnit}
                         units={TIME_UNITS}
                         clearable />
          <p>
            Graylog sends Notifications for Alerts every time they occur. Set a Grace Period to control how long
            Graylog should wait before sending Notifications again. Note that Events with keys will have a Grace
            Period for each different key value.
          </p>
        </FormGroup>

        <FormGroup>
          <ControlLabel>Message Backlog</ControlLabel>
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
                         disabled={!isBacklogSizeEnabled} />
          </InputGroup>
          <p>Number of messages to be included in Notifications.</p>
        </FormGroup>
      </Container>
    );
  }
}

export default NotificationSettingsForm;
