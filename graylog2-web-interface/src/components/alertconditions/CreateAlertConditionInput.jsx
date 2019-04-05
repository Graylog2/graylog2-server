import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import Reflux from 'reflux';
import naturalSort from 'javascript-natural-sort';
import { Button, Col, Row } from 'react-bootstrap';

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
    return streams.find(s => s.id === streamId);
  },

  _onStreamChange(nextStream) {
    this.setState({ selectedStream: this._findStream(this.state.streams, nextStream) });
  },

  _onSubmit(data) {
    if (!this.state.selectedStream) {
      UserNotification.error('Please select the stream that the condition should check.', 'Could not save condition');
    }

    AlertConditionsActions.save(this.state.selectedStream.id, data).then(() => {
      history.push(Routes.stream_alerts(this.state.selectedStream.id));
    });
  },

  _openForm() {
    this.configurationForm.open();
  },

  _resetForm() {
    this.setState({ type: this.PLACEHOLDER });
  },

  _formatConditionForm(type) {
    return (
      <AlertConditionForm ref={(configurationForm) => { this.configurationForm = configurationForm; }}
                          onCancel={this._resetForm}
                          onSubmit={this._onSubmit}
                          conditionType={this.state.availableConditions[type]} />
    );
  },

  _formatOption(key, value) {
    return { value: value, label: key };
  },

  _isLoading() {
    return !this.state.availableConditions || !this.state.streams;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const conditionForm = (this.state.type !== this.PLACEHOLDER ? this._formatConditionForm(this.state.type) : null);
    const availableTypes = Object.keys(this.state.availableConditions).map((value) => {
      return <option key={`type-option-${value}`} value={value}>{this.state.availableConditions[value].name}</option>;
    });
    const formattedStreams = this.state.streams
      .map(stream => this._formatOption(stream.title, stream.id))
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
                        value={this.state.selectedStream ? this.state.selectedStream.id : undefined} />
              </Input>

              <Input id="condition-type-selector"
                     type="select"
                     value={this.state.type}
                     onChange={this._onChange}
                     disabled={!this.state.selectedStream}
                     label="Condition type"
                     help="Select the condition type that will be used.">
                <option value={this.PLACEHOLDER} disabled>Select a condition type</option>
                {availableTypes}
              </Input>
              {conditionForm}
              {' '}
              <Button onClick={this._openForm} disabled={this.state.type === this.PLACEHOLDER} bsStyle="success">
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
