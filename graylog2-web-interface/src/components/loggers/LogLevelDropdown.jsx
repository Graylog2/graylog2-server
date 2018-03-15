import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { DropdownButton, MenuItem } from 'react-bootstrap';
import lodash from 'lodash';

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
