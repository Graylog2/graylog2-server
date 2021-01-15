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
import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import naturalSort from 'javascript-natural-sort';

import { Col, Row, Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import { ExternalLinkButton, Select, Spinner } from 'components/common';
import { ConfigurationForm } from 'components/configurationforms';
import Routes from 'routing/Routes';
import UserNotification from 'util/UserNotification';
import history from 'util/History';
import CombinedProvider from 'injection/CombinedProvider';

const { AlertNotificationsStore, AlertNotificationsActions } = CombinedProvider.get('AlertNotifications');
const { AlarmCallbacksActions } = CombinedProvider.get('AlarmCallbacks');
const { StreamsStore } = CombinedProvider.get('Streams');

const CreateAlertNotificationInput = createReactClass({
  displayName: 'CreateAlertNotificationInput',
  propTypes: {
    initialSelectedStream: PropTypes.string,
  },
  mixins: [Reflux.connect(AlertNotificationsStore)],

  getDefaultProps() {
    return {
      initialSelectedStream: undefined,
    };
  },

  getInitialState() {
    return {
      streams: undefined,
      selectedStream: undefined,
      type: this.PLACEHOLDER,
    };
  },

  componentDidMount() {
    StreamsStore.listStreams().then((streams) => {
      const nextState = { streams: streams };
      const { initialSelectedStream } = this.props;

      if (initialSelectedStream) {
        nextState.selectedStream = this._findStream(streams, initialSelectedStream);
      }

      this.setState(nextState);
    });

    AlertNotificationsActions.available();
  },

  PLACEHOLDER: 'placeholder',

  _onChange(evt) {
    this.setState({ type: evt.target.value });
  },

  _findStream(streams, streamId) {
    return streams.find((s) => s.id === streamId);
  },

  _onStreamChange(nextStream) {
    const { streams } = this.state;

    this.setState({ selectedStream: this._findStream(streams, nextStream) });
  },

  _onSubmit(data) {
    const { selectedStream } = this.state;

    if (!selectedStream) {
      UserNotification.error('Please select the stream that the condition should check.', 'Could not save condition');
    }

    AlarmCallbacksActions.save(selectedStream.id, data).then(
      () => history.push(Routes.stream_alerts(selectedStream.id)),
      () => this.configurationForm.open(),
    );
  },

  _openForm() {
    this.configurationForm.open();
  },

  _resetForm() {
    this.setState({ type: this.PLACEHOLDER });
  },

  _formatNotificationForm(type) {
    const { availableNotifications } = this.state;
    const typeDefinition = availableNotifications[type];

    return (
      <ConfigurationForm ref={(configurationForm) => { this.configurationForm = configurationForm; }}
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
    const { availableNotifications, streams } = this.state;

    return !availableNotifications || !streams;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { type, availableNotifications, streams, selectedStream } = this.state;

    const notificationForm = (type !== this.PLACEHOLDER ? this._formatNotificationForm(type) : null);
    const availableTypes = Object.keys(availableNotifications).map((value) => {
      return (
        <option key={`type-option-${value}`} value={value}>
          {availableNotifications[value].name}
        </option>
      );
    });
    const formattedStreams = streams
      .map((stream) => this._formatOption(stream.title, stream.id))
      .sort((s1, s2) => naturalSort(s1.label.toLowerCase(), s2.label.toLowerCase()));

    const notificationTypeHelp = (
      <span>
        Select the notification type that will be used. You can find more types in the{' '}
        <a href="https://marketplace.graylog.org/" target="_blank" rel="noopener noreferrer">Graylog Marketplace</a>.
      </span>
    );

    return (
      <div>
        <ExternalLinkButton href="https://marketplace.graylog.org/"
                            bsStyle="info"
                            className="pull-right">
          Find more notifications
        </ExternalLinkButton>

        <h2>Notification</h2>
        <p className="description">
          Define the notification that will be triggered from the alert conditions in a stream.
        </p>

        <Row>
          <Col md={6}>
            <form>
              <Input id="stream-selector"
                     label="Notify on stream"
                     help="Select the stream that should use this notification when its alert conditions are triggered.">
                <Select placeholder="Select a stream"
                        options={formattedStreams}
                        onChange={this._onStreamChange}
                        value={selectedStream ? selectedStream.id : undefined} />
              </Input>

              <Input id="notification-type-selector"
                     type="select"
                     value={type}
                     onChange={this._onChange}
                     disabled={!selectedStream}
                     label="Notification type"
                     help={notificationTypeHelp}>
                <option value={this.PLACEHOLDER} disabled>Select a notification type</option>
                {availableTypes}
              </Input>
              {notificationForm}
              {' '}
              <Button onClick={this._openForm} disabled={type === this.PLACEHOLDER} bsStyle="success">
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
