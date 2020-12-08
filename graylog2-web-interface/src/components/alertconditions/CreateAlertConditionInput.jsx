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
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import Reflux from 'reflux';
import naturalSort from 'javascript-natural-sort';

import { Col, Row, Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import { Select, Spinner } from 'components/common';
import { AlertConditionForm } from 'components/alertconditions';
import Routes from 'routing/Routes';
import UserNotification from 'util/UserNotification';
import history from 'util/History';
import CombinedProvider from 'injection/CombinedProvider';

const { AlertConditionsStore, AlertConditionsActions } = CombinedProvider.get('AlertConditions');
const { StreamsStore } = CombinedProvider.get('Streams');

const CreateAlertConditionInput = createReactClass({
  displayName: 'CreateAlertConditionInput',
  propTypes: {
    initialSelectedStream: PropTypes.string,
  },
  mixins: [Reflux.connect(AlertConditionsStore)],

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

    AlertConditionsActions.available();
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

    AlertConditionsActions.save(selectedStream.id, data).then(() => {
      history.push(Routes.stream_alerts(selectedStream.id));
    });
  },

  _openForm() {
    this.configurationForm.open();
  },

  _resetForm() {
    this.setState({ type: this.PLACEHOLDER });
  },

  _formatConditionForm(type) {
    const { availableConditions } = this.state;

    return (
      <AlertConditionForm ref={(configurationForm) => { this.configurationForm = configurationForm; }}
                          onCancel={this._resetForm}
                          onSubmit={this._onSubmit}
                          conditionType={availableConditions[type]} />
    );
  },

  _formatOption(key, value) {
    return { value: value, label: key };
  },

  _isLoading() {
    const { availableConditions, streams } = this.state;

    return !availableConditions || !streams;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { availableConditions, selectedStream, streams, type } = this.state;

    const conditionForm = (type !== this.PLACEHOLDER ? this._formatConditionForm(type) : null);
    const availableTypes = Object.keys(availableConditions).map((value) => {
      return <option key={`type-option-${value}`} value={value}>{availableConditions[value].name}</option>;
    });
    const formattedStreams = streams
      .map((stream) => this._formatOption(stream.title, stream.id))
      .sort((s1, s2) => naturalSort(s1.label.toLowerCase(), s2.label.toLowerCase()));

    return (
      <div>
        <h2>Condition</h2>
        <p className="description">Define the condition to evaluate when triggering a new alert.</p>

        <Row>
          <Col md={6}>
            <form>
              <Input id="stream-selector" label="Alert on stream" help="Select the stream that the condition will use to trigger alerts.">
                <Select placeholder="Select a stream"
                        options={formattedStreams}
                        onChange={this._onStreamChange}
                        value={selectedStream ? selectedStream.id : undefined} />
              </Input>

              <Input id="condition-type-selector"
                     type="select"
                     value={type}
                     onChange={this._onChange}
                     disabled={!selectedStream}
                     label="Condition type"
                     help="Select the condition type that will be used.">
                <option value={this.PLACEHOLDER} disabled>Select a condition type</option>
                {availableTypes}
              </Input>
              {conditionForm}
              {' '}
              <Button onClick={this._openForm} disabled={type === this.PLACEHOLDER} bsStyle="success">
                Add alert condition
              </Button>
            </form>
          </Col>
        </Row>
      </div>
    );
  },
});

export default CreateAlertConditionInput;
