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
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import lodash from 'lodash';

import { DropdownButton, MenuItem } from 'components/graylog';
import CombinedProvider from 'injection/CombinedProvider';

const { LoggersStore, LoggersActions } = CombinedProvider.get('Loggers');

const LogLevelDropdown = createReactClass({
  displayName: 'LogLevelDropdown',

  propTypes: {
    name: PropTypes.string.isRequired,
    nodeId: PropTypes.string.isRequired,
    subsystem: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(LoggersStore)],

  _changeLoglevel(loglevel) {
    LoggersActions.setSubsystemLoggerLevel(this.props.nodeId, this.props.name, loglevel);
  },

  _menuLevelClick(loglevel) {
    return (event) => {
      event.preventDefault();
      this._changeLoglevel(loglevel);
    };
  },

  render() {
    const { subsystem, nodeId } = this.props;
    const loglevels = this.state.availableLoglevels
      .map((loglevel) => {
        return (
          <MenuItem key={`${subsystem}-${nodeId}-${loglevel}`}
                    active={subsystem.level === loglevel}
                    onClick={this._menuLevelClick(loglevel)}>
            {lodash.capitalize(loglevel)}
          </MenuItem>
        );
      });

    return (
      <DropdownButton id="loglevel" bsSize="xsmall" title={lodash.capitalize(subsystem.level)}>
        {loglevels}
      </DropdownButton>
    );
  },
});

export default LogLevelDropdown;
