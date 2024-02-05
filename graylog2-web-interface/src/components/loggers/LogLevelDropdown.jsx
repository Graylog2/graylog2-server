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
import PropTypes from 'prop-types';
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import capitalize from 'lodash/capitalize';

import { DropdownButton, MenuItem } from 'components/bootstrap';
import { LoggersActions, LoggersStore } from 'stores/system/LoggersStore';
import withTelemetry from 'logic/telemetry/withTelemetry';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import withLocation from 'routing/withLocation';

const LogLevelDropdown = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  displayName: 'LogLevelDropdown',

  // eslint-disable-next-line react/no-unused-class-component-methods
  propTypes: {
    name: PropTypes.string.isRequired,
    nodeId: PropTypes.string.isRequired,
    subsystem: PropTypes.object.isRequired,
    sendTelemetry: PropTypes.func.isRequired,
    location: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(LoggersStore)],

  _changeLoglevel(loglevel) {
    LoggersActions.setSubsystemLoggerLevel(this.props.nodeId, this.props.name, loglevel);
  },

  _menuLevelClick(loglevel) {
    return (event) => {
      event.preventDefault();
      this._changeLoglevel(loglevel);

      this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.LOGGING.LOG_LEVEL_EDITED, {
        app_pathname: getPathnameWithoutId(this.props.location.pathname),
        app_action_value: 'log-level-change',
        event_details: { value: loglevel },
      });
    };
  },

  render() {
    const { subsystem, nodeId } = this.props;
    const loglevels = this.state.availableLoglevels
      .map((loglevel) => (
        <MenuItem key={`${subsystem}-${nodeId}-${loglevel}`}
                  active={subsystem.level === loglevel}
                  onClick={this._menuLevelClick(loglevel)}>
          {capitalize(loglevel)}
        </MenuItem>
      ));

    return (
      <DropdownButton id="loglevel" bsSize="xsmall" title={capitalize(subsystem.level)}>
        {loglevels}
      </DropdownButton>
    );
  },
});

export default withLocation(withTelemetry(LogLevelDropdown));
