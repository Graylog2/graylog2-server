import React from 'react';

import AlarmCallback from 'components/alarmcallbacks//AlarmCallback';
import Spinner from 'components/common/Spinner';

const AlarmCallbackList = React.createClass({
  propTypes: {
    alarmCallbacks: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    types: React.PropTypes.object.isRequired,
    streamId: React.PropTypes.string.isRequired,
    permissions: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
    onUpdate: React.PropTypes.func.isRequired,
    onDelete: React.PropTypes.func.isRequired,
  },
  _humanReadableType(alarmCallback) {
    if (this.props.availableAlarmCallbacks) {
      const available = this.props.availableAlarmCallbacks[alarmCallback.type];

      if (available) {
        return available.name;
      } else {
        return 'Unknown callback type';
      }
    }
    return <Spinner />;
  },
  render() {
    var alarmCallbacks = this.props.alarmCallbacks.map((alarmCallback) => {
      return (<AlarmCallback key={"alarmCallback-" + alarmCallback.id} alarmCallback={alarmCallback} streamId={this.props.streamId}
                            types={this.props.types} permissions={this.props.permissions}
                            deleteAlarmCallback={this.props.onDelete} updateAlarmCallback={this.props.onUpdate} />);
    });

    if (alarmCallbacks.length > 0) {
      return (
        <div className="alert-callbacks">
          {alarmCallbacks}
        </div>
      );
    } else {
      return (
        <div className="alert alert-info no-alarm-callbacks">
          No configured alarm callbacks.
        </div>
      );
    }
  },
});

export default AlarmCallbackList;
