import React from 'react';
import Reflux from 'reflux';
import { DropdownButton, MenuItem } from 'react-bootstrap';
import String from 'string';

import LoggersActions from 'actions/system/LoggersActions';
import LoggersStore from 'stores/system/LoggersStore';

const LogLevelDropdown = React.createClass({
  propTypes: {
    name: React.PropTypes.string.isRequired,
    nodeId: React.PropTypes.string.isRequired,
    subsystem: React.PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(LoggersStore)],
  _changeLoglevel(loglevel) {
    LoggersActions.setSubsystemLoggerLevel(this.props.nodeId, this.props.name, loglevel);
  },
  render() {
    const { subsystem, nodeId } = this.props;
    const loglevels = this.state.availableLoglevels
      .map((loglevel) =>
        <MenuItem key={subsystem + '-' + nodeId + '-' + loglevel} active={subsystem.level === loglevel} onClick={(evt) => { evt.preventDefault(); this._changeLoglevel(loglevel); }}>
          {String(loglevel).capitalize().toString()}
        </MenuItem>);
    return (
      <DropdownButton id="loglevel" bsSize="xsmall" title={subsystem.level}>
        {loglevels}
      </DropdownButton>
    );
  },
});

export default LogLevelDropdown;
