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
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';

import { Row, Col } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import PermissionsMixin from 'util/PermissionsMixin';
import Spinner from 'components/common/Spinner';
import StreamsStore from 'stores/streams/StreamsStore';
import { OutputsStore } from 'stores/outputs/OutputsStore';
import withTelemetry from 'logic/telemetry/withTelemetry';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import withLocation from 'routing/withLocation';

import OutputList from './OutputList';
import CreateOutputDropdown from './CreateOutputDropdown';
import AssignOutputDropdown from './AssignOutputDropdown';

const OutputsComponent = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  displayName: 'OutputsComponent',

  // eslint-disable-next-line react/no-unused-class-component-methods
  propTypes: {
    streamId: PropTypes.string.isRequired,
    permissions: PropTypes.array.isRequired,
    sendTelemetry: PropTypes.func.isRequired,
    location: PropTypes.object.isRequired,
  },

  mixins: [PermissionsMixin],

  getInitialState() {
    return {};
  },

  componentDidMount() {
    this.loadData();
  },

  loadData() {
    const callback = (resp) => {
      this.setState({
        outputs: resp.outputs,
      });

      if (this.props.streamId) {
        this._fetchAssignableOutputs(resp.outputs);
      }
    };

    if (this.props.streamId) {
      OutputsStore.loadForStreamId(this.props.streamId, callback);
    } else {
      OutputsStore.load(callback);
    }

    OutputsStore.loadAvailableTypes((resp) => {
      this.setState({ types: resp.types });
    });
  },

  _handleUpdate() {
    this.loadData();
  },

  _handleCreateOutput(data) {
    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_CREATED, {
      app_pathname: getPathnameWithoutId(this.props.location.pathname),
      app_action_value: 'create-output',
    });

    OutputsStore.save(data, (result) => {
      if (this.props.streamId) {
        StreamsStore.addOutput(this.props.streamId, result.id, (response) => {
          this._handleUpdate();

          return response;
        });
      } else {
        this._handleUpdate();
      }

      return result;
    });
  },

  _fetchAssignableOutputs(outputs) {
    OutputsStore.load((resp) => {
      const streamOutputIds = outputs.map((output) => output.id);
      const assignableOutputs = resp.outputs
        .filter((output) => streamOutputIds.indexOf(output.id) === -1)
        .sort((output1, output2) => output1.title.localeCompare(output2.title));

      this.setState({ assignableOutputs: assignableOutputs });
    });
  },

  _handleAssignOutput(outputId) {
    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_ASSIGNED, {
      app_pathname: getPathnameWithoutId(this.props.location.pathname),
      app_action_value: 'assign-output',
    });

    StreamsStore.addOutput(this.props.streamId, outputId, (response) => {
      this._handleUpdate();

      return response;
    });
  },

  _removeOutputGlobally(outputId) {
    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_GLOBALLY_REMOVED, {
      app_pathname: getPathnameWithoutId(this.props.location.pathname),
      app_action_value: 'globally-remove-output',
    });

    // eslint-disable-next-line no-alert
    if (window.confirm('Do you really want to terminate this output?')) {
      OutputsStore.remove(outputId, (response) => {
        UserNotification.success('Output was terminated.', 'Success');
        this._handleUpdate();

        return response;
      });
    }
  },

  _removeOutputFromStream(outputId, streamId) {
    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_FROM_STREAM_REMOVED, {
      app_pathname: getPathnameWithoutId(this.props.location.pathname),
      app_action_value: 'remove-output-from-stream',
    });

    // eslint-disable-next-line no-alert
    if (window.confirm('Do you really want to remove this output from the stream?')) {
      StreamsStore.removeOutput(streamId, outputId, (response) => {
        UserNotification.success('Output was removed from stream.', 'Success');
        this._handleUpdate();

        return response;
      });
    }
  },

  _handleOutputUpdate(output, deltas) {
    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_UPDATED, {
      app_pathname: getPathnameWithoutId(this.props.location.pathname),
      app_action_value: 'output-update',
    });

    OutputsStore.update(output, deltas, () => {
      this._handleUpdate();
    });
  },

  render() {
    if (this.state.outputs && this.state.types && (!this.props.streamId || this.state.assignableOutputs)) {
      const { permissions } = this.props;
      const { streamId } = this.props;
      const createOutputDropdown = (this.isPermitted(permissions, ['outputs:create'])
        ? (
          <CreateOutputDropdown types={this.state.types}
                                onSubmit={this._handleCreateOutput}
                                getTypeDefinition={OutputsStore.loadAvailable}
                                streamId={streamId} />
        ) : null);
      const assignOutputDropdown = (streamId
        ? (
          <AssignOutputDropdown streamId={streamId}
                                outputs={this.state.assignableOutputs}
                                onSubmit={this._handleAssignOutput} />
        ) : null);

      return (
        <div className="outputs">
          <Row className="content">
            <Col md={4}>
              {createOutputDropdown}
            </Col>
            <Col md={8}>
              {assignOutputDropdown}
            </Col>
          </Row>

          <OutputList streamId={streamId}
                      outputs={this.state.outputs}
                      permissions={permissions}
                      getTypeDefinition={OutputsStore.loadAvailable}
                      types={this.state.types}
                      onRemove={this._removeOutputFromStream}
                      onTerminate={this._removeOutputGlobally}
                      onUpdate={this._handleOutputUpdate} />
        </div>
      );
    }

    return <Spinner />;
  },
});

export default withLocation(withTelemetry(OutputsComponent));
