import PropTypes from 'prop-types';
import React from 'react';
import Reflux from 'reflux';
import { DropdownButton, MenuItem } from 'react-bootstrap';
import lodash from 'lodash';

import ActionsProvider from 'injection/ActionsProvider';
const LoggersActions = ActionsProvider.getActions('Loggers');

import StoreProvider from 'injection/StoreProvider';
const LoggersStore = StoreProvider.getStore('Loggers');

const LogLevelDropdown = React.createClass({
  propTypes: {
    name: PropTypes.string.isRequired,
    nodeId: PropTypes.string.isRequired,
    subsystem: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(LoggersStore)],
  _changeLoglevel(loglevel) {
    LoggersActions.setSubsystemLoggerLevel(this.props.nodeId, this.props.name, loglevel);
  },
  render() {
    const { subsystem, nodeId } = this.props;
    const loglevels = this.state.availableLoglevels
      .map(loglevel =>
        <MenuItem key={`${subsystem}-${nodeId}-${loglevel}`} active={subsystem.level === loglevel} onClick={(evt) => { evt.preventDefault(); this._changeLoglevel(loglevel); }}>
          {lodash.capitalize(loglevel)}
        </MenuItem>);
    return (
      <DropdownButton id="loglevel" bsSize="xsmall" title={lodash.capitalize(subsystem.level)}>
        {loglevels}
      </DropdownButton>
    );
  },
});

export default LogLevelDropdown;
