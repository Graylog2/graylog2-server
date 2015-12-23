import React from 'react';
import { Alert } from 'react-bootstrap';

import { AlarmCallback } from 'components/alarmcallbacks';

const AlarmCallbackList = React.createClass({
  propTypes: {
    alarmCallbacks: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    types: React.PropTypes.object.isRequired,
    streamId: React.PropTypes.string.isRequired,
    onUpdate: React.PropTypes.func.isRequired,
    onDelete: React.PropTypes.func.isRequired,
  },
  render() {
    const alarmCallbacks = this.props.alarmCallbacks.map((alarmCallback) => {
      return (
        <li key={'alarmCallback-' + alarmCallback.id}>
          <AlarmCallback alarmCallback={alarmCallback} streamId={this.props.streamId}
                         types={this.props.types} deleteAlarmCallback={this.props.onDelete}
                         updateAlarmCallback={this.props.onUpdate}/>
        </li>
      );
    });

    if (alarmCallbacks.length > 0) {
      return (
        <div className="alert-callbacks">
          <ul className="alarm-callbacks">
            {alarmCallbacks}
          </ul>
        </div>
      );
    }

    return (
      <Alert bsStule="info" className="no-alarm-callbacks">
        No configured alarm callbacks.
      </Alert>
    );
  },
});

export default AlarmCallbackList;
