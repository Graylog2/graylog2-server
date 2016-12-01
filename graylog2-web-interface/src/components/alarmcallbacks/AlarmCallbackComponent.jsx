import React from 'react';
import Reflux from 'reflux';

import CombinedProvider from 'injection/CombinedProvider';
const { AlarmCallbacksStore, AlarmCallbacksActions } = CombinedProvider.get('AlarmCallbacks');

import { IfPermitted, Spinner } from 'components/common';
import { AlarmCallbackList, CreateAlarmCallbackButton } from 'components/alarmcallbacks';

const AlarmCallbackComponent = React.createClass({
  propTypes: {
    streamId: React.PropTypes.string.isRequired,
  },
  mixins: [Reflux.connect(AlarmCallbacksStore)],
  getInitialState() {
    return {};
  },
  componentDidMount() {
    this.loadData();
  },
  loadData() {
    AlarmCallbacksActions.list.triggerPromise(this.props.streamId).then((alarmCallbacks) => {
      this.setState({alarmCallbacks: alarmCallbacks});
    });
    AlarmCallbacksActions.available();
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
    if (!this.state.alarmCallbacks || !this.state.availableAlarmCallbacks) {
      return <Spinner />;
    }

    return (
      <div className="alarm-callback-component">
        <IfPermitted permissions={'streams:edit:' + this.props.streamId}>
          <CreateAlarmCallbackButton streamId={this.props.streamId} types={this.state.availableAlarmCallbacks} onCreate={this._createAlarmCallback} />
        </IfPermitted>

        <AlarmCallbackList alarmCallbacks={this.state.alarmCallbacks}
                           streamId={this.props.streamId} types={this.state.availableAlarmCallbacks}
                           onUpdate={this._updateAlarmCallback} onDelete={this._deleteAlarmCallback} onCreate={this._createAlarmCallback} />
      </div>
    );
  },
});

export default AlarmCallbackComponent;
