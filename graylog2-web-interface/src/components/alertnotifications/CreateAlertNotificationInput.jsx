import React from 'react';
import Reflux from 'reflux';
import naturalSort from 'javascript-natural-sort';
import { Button, Col, Row } from 'react-bootstrap';

import { Input } from 'components/bootstrap';
import { Select, Spinner } from 'components/common';
import { ConfigurationForm } from 'components/configurationforms';
import Routes from 'routing/Routes';
import UserNotification from 'util/UserNotification';
import history from 'util/History';

import CombinedProvider from 'injection/CombinedProvider';
const { AlertNotificationsStore, AlertNotificationsActions } = CombinedProvider.get('AlertNotifications');
const { AlarmCallbacksActions } = CombinedProvider.get('AlarmCallbacks');
const { StreamsStore } = CombinedProvider.get('Streams');

const CreateAlertNotificationInput = React.createClass({
  mixins: [Reflux.connect(AlertNotificationsStore)],
  getInitialState() {
    return {
      streams: undefined,
      selectedStream: undefined,
      type: this.PLACEHOLDER,
    };
  },

  componentDidMount() {
    StreamsStore.listStreams().then((streams) => {
      this.setState({ streams: streams });
    });
    AlertNotificationsActions.available();
  },

  PLACEHOLDER: 'placeholder',

  _onChange(evt) {
    this.setState({ type: evt.target.value });
  },

  _onStreamChange(nextStream) {
    this.setState({ selectedStream: this.state.streams.find(s => s.id === nextStream) });
  },

  _onSubmit(data) {
    if (!this.state.selectedStream) {
      UserNotification.error('Please select the stream that the condition should check.', 'Could not save condition');
    }

    AlarmCallbacksActions.save(this.state.selectedStream.id, data).then(
      () => {
        history.pushState(null, Routes.ALERTS.NOTIFICATIONS);
      },
      () => this.refs.configurationForm.open(),
    );
  },
  _openForm() {
    this.refs.configurationForm.open();
  },
  _resetForm() {
    this.setState({ type: this.PLACEHOLDER });
  },
  _formatNotificationForm(type) {
    const typeDefinition = this.state.availableNotifications[type];
    return (
      <ConfigurationForm ref="configurationForm"
                         key="configuration-form-output"
                         configFields={typeDefinition.requested_configuration}
                         title={`Create new ${typeDefinition.name}`}
                         typeName={type}
                         submitAction={this._onSubmit}
                         cancelAction={this._resetForm} />
    );
  },

  _formatOption(key, value) {
    return { value: value, label: key };
  },

  _isLoading() {
    return !this.state.availableNotifications || !this.state.streams;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const notificationForm = (this.state.type !== this.PLACEHOLDER ? this._formatNotificationForm(this.state.type) : null);
    const availableTypes = Object.keys(this.state.availableNotifications).map((value) => {
      return (
        <option key={`type-option-${value}`} value={value}>
          {this.state.availableNotifications[value].name}
        </option>
      );
    });
    const formattedStreams = this.state.streams
      .map(stream => this._formatOption(stream.title, stream.id))
      .sort((s1, s2) => naturalSort(s1.label.toLowerCase(), s2.label.toLowerCase()));
    return (
      <div>
        <h2>Notification</h2>
        <p className="description">
          Define the notification that will be triggered from the alert conditions in a stream.
        </p>

        <Row>
          <Col md={6}>
            <form>
              <Input label="Notify on stream"
                     help="Select the stream that should use this notification when its alert conditions are triggered.">
                <Select placeholder="Select a stream" options={formattedStreams} onValueChange={this._onStreamChange} />
              </Input>

              <Input type="select" value={this.state.type} onChange={this._onChange}
                     disabled={!this.state.selectedStream}
                     label="Notification type" help="Select the notification type that will be used.">
                <option value={this.PLACEHOLDER} disabled>Select a notification type</option>
                {availableTypes}
              </Input>
              {notificationForm}
              {' '}
              <Button onClick={this._openForm} disabled={this.state.type === this.PLACEHOLDER} bsStyle="success">
                Add alert notification
              </Button>
            </form>
          </Col>
        </Row>
      </div>
    );
  },
});

export default CreateAlertNotificationInput;
