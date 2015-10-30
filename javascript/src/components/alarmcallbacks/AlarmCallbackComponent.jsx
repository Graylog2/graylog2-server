import React from 'react';

import AlarmCallbacksActions from 'actions/alarmcallbacks/AlarmCallbacksActions';
import PermissionsMixin from 'util/PermissionsMixin';

import Spinner from 'components/common/Spinner';
import AlarmCallbackList from 'components/alarmcallbacks/AlarmCallbackList';
import CreateAlarmCallbackButton from 'components/alarmcallbacks/CreateAlarmCallbackButton';

var AlarmCallbackComponent = React.createClass({
  propTypes: {
    streamId: React.PropTypes.string.isRequired,
    permissions: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
  },
  mixins: [PermissionsMixin],
  getInitialState() {
    return {
    };
  },
  componentDidMount() {
    this.loadData();
  },
  loadData() {
    AlarmCallbacksActions.list.triggerPromise(this.props.streamId).then((alarmCallbacks) => {
      this.setState({alarmCallbacks: alarmCallbacks});
    });
    AlarmCallbacksActions.available.triggerPromise(this.props.streamId).then((available) => {
      this.setState({availableAlarmCallbacks: available});
    }, (error) => {
      console.log(error);
    });
  },
  _deleteAlarmCallback(alarmCallback) {
    AlarmCallbacksActions.delete.triggerPromise(this.props.streamId, alarmCallback.id).then(() => {
      this.loadData();
    });
  },
  _createAlarmCallback(data) {
    AlarmCallbacksActions.save.triggerPromise(this.props.streamId, data).then(() => {
      this.loadData();
    });
  },
  _updateAlarmCallback(alarmCallback, data) {
    AlarmCallbacksActions.update.triggerPromise(this.props.streamId, alarmCallback.id, data).then(() => {
      this.loadData();
    });
  },
  render() {
    var permissions = this.props.permissions;
    if (this.state.alarmCallbacks && this.state.availableAlarmCallbacks) {
      const createAlarmCallbackButton = (this.isPermitted(permissions, ['streams:edit:' + this.props.streamId]) ?
        <CreateAlarmCallbackButton streamId={this.props.streamId} types={this.state.availableAlarmCallbacks} onCreate={this._createAlarmCallback} /> : null);
      return (
        <div className="alarm-callback-component">
          {createAlarmCallbackButton}

          <AlarmCallbackList alarmCallbacks={this.state.alarmCallbacks}
                             streamId={this.props.streamId} permissions={permissions} types={this.state.availableAlarmCallbacks}
                             onUpdate={this._updateAlarmCallback} onDelete={this._deleteAlarmCallback} onCreate={this._createAlarmCallback} />
        </div>
      );
    }

    return <Spinner />;
  },
});

export default AlarmCallbackComponent;
