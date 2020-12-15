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
import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import naturalSort from 'javascript-natural-sort';

import { Link } from 'components/graylog/router';
import { ControlLabel, FormGroup, HelpBlock, Button } from 'components/graylog';
import { SelectableList } from 'components/common';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import Routes from 'routing/Routes';

const PipelineConnectionsForm = createReactClass({
  displayName: 'PipelineConnectionsForm',

  propTypes: {
    pipeline: PropTypes.object.isRequired,
    connections: PropTypes.array.isRequired,
    streams: PropTypes.array.isRequired,
    save: PropTypes.func.isRequired,
  },

  getInitialState() {
    return {
      connectedStreams: this._getFormattedStreams(this._getConnectedStreams(this.props.pipeline, this.props.connections, this.props.streams)),
    };
  },

  openModal() {
    this.modal.open();
  },

  _onStreamsChange(newStreams) {
    this.setState({ connectedStreams: newStreams.sort((s1, s2) => naturalSort(s1.label, s2.label)) });
  },

  _closeModal() {
    this.modal.close();
  },

  _resetForm() {
    this.setState(this.getInitialState());
  },

  _saved() {
    this._closeModal();
  },

  _save() {
    const streamIds = this.state.connectedStreams.map((cs) => cs.value);
    const newConnection = {
      pipeline: this.props.pipeline.id,
      streams: streamIds,
    };

    this.props.save(newConnection, this._saved);
  },

  _getConnectedStreams(pipeline, connections, streams) {
    return connections
      .filter((c) => c.pipeline_ids && c.pipeline_ids.includes(pipeline.id)) // Get connections for this pipeline
      .filter((c) => streams.some((s) => s.id === c.stream_id)) // Filter out deleted streams
      .map((c) => this.props.streams.find((s) => s.id === c.stream_id));
  },

  _getFormattedStreams(streams) {
    return streams
      .map((s) => {
        return { value: s.id, label: s.title };
      })
      .sort((s1, s2) => naturalSort(s1.label, s2.label));
  },

  _getFilteredStreams(streams) {
    return streams.filter((s) => !this.state.connectedStreams.some((cs) => cs.value.toLowerCase() === s.id.toLowerCase()));
  },

  render() {
    const streamsHelp = (
      <span>
        Select the streams you want to connect to this pipeline, or create one in the{' '}
        <Link to={Routes.STREAMS}>Streams page</Link>.
      </span>
    );

    return (
      <span>
        <Button onClick={this.openModal} bsStyle="info">
          <span>Edit connections</span>
        </Button>
        <BootstrapModalForm ref={(modal) => { this.modal = modal; }}
                            title={<span>Edit connections for <em>{this.props.pipeline.title}</em></span>}
                            onSubmitForm={this._save}
                            onCancel={this._resetForm}
                            submitButtonText="Save">
          <fieldset>
            <FormGroup id="streamsConnections">
              <ControlLabel>Streams</ControlLabel>
              <SelectableList options={this._getFormattedStreams(this._getFilteredStreams(this.props.streams))}
                              onChange={this._onStreamsChange}
                              selectedOptionsType="object"
                              selectedOptions={this.state.connectedStreams} />
              <HelpBlock>{streamsHelp}</HelpBlock>
            </FormGroup>
          </fieldset>
        </BootstrapModalForm>
      </span>
    );
  },
});

export default PipelineConnectionsForm;
