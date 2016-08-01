import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import { Input, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { Select, SelectableList } from 'components/common';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import ObjectUtils from 'util/ObjectUtils';

import PipelinesStore from 'pipelines/PipelinesStore';

import Routes from 'routing/Routes';

const ConnectionForm = React.createClass({
  propTypes: {
    stream: PropTypes.object,
    connection: PropTypes.object,
    streams: PropTypes.array,
    create: PropTypes.bool,
    save: PropTypes.func.isRequired,
    validateConnection: PropTypes.func.isRequired,
  },

  mixins: [Reflux.connect(PipelinesStore)],

  getDefaultProps() {
    return {
      streams: [],
      connection: {
        stream: undefined,
        pipelines: [],
      },
    };
  },

  getInitialState() {
    const connection = ObjectUtils.clone(this.props.connection);
    return {
      // when editing, take the connection that's been passed in
      connection: {
        stream: connection.stream,
        pipelines: this._getFormattedOptions(connection.pipelines),
      },
    };
  },

  openModal() {
    this.refs.modal.open();
  },

  _onStreamChange(newStream) {
    const connection = ObjectUtils.clone(this.state.connection);
    connection.stream = this.props.streams.filter(s => s.id === newStream)[0];
    this.setState({ connection });
  },

  _onConnectionsChange(newRules) {
    const connection = ObjectUtils.clone(this.state.connection);
    connection.pipelines = newRules;
    this.setState({ connection });
  },

  _closeModal() {
    this.refs.modal.close();
  },

  _saved() {
    this._closeModal();
    if (this.props.create) {
      this.setState(this.getInitialState());
    }
  },

  _save() {
    const connection = this.state.connection;
    const filteredConnection = {};
    filteredConnection.stream = connection.stream.id;
    filteredConnection.pipelines = connection.pipelines.map(p => p.value);
    this.props.save(filteredConnection, this._saved);
  },

  _getFormattedStreams(streams) {
    if (!streams) {
      return [];
    }

    return streams.map(s => {
      return { value: s.id, label: s.title };
    });
  },

  _getFormattedOptions(pipelines) {
    if (!pipelines) {
      return [];
    }

    return pipelines
      .map(pipeline => {
        return { value: pipeline.id, label: pipeline.title };
      });
  },

  _getFilteredFormattedOptions(pipelines) {
    return this._getFormattedOptions(pipelines)
      // Remove already selected options
      .filter(pipeline => !this.state.connection.pipelines.some(p => p.value === pipeline.value));
  },

  render() {
    let triggerButtonContent;
    if (this.props.create) {
      triggerButtonContent = 'Add new connection';
    } else {
      triggerButtonContent = <span>Edit connections</span>;
    }

    let streamSelector;
    if (this.props.create) {
      const streamHelp = (
        <span>
          Select the stream you want to connect pipelines to, or create one in the{' '}
          <LinkContainer to={Routes.STREAMS}><a>Streams page</a></LinkContainer>.
        </span>
      );
      streamSelector = (
        <Input label="Stream"
               help={streamHelp}>
          <Select options={this._getFormattedStreams(this.props.streams)} onValueChange={this._onStreamChange} />
        </Input>
      );
    }

    const pipelineHelp = (
      <span>
        Select the pipelines to connect to this stream, or create one in the{' '}
        <LinkContainer to={Routes.pluginRoute('SYSTEM_PIPELINES_OVERVIEW')}><a>Pipelines Overview page</a></LinkContainer>.
      </span>
    );

    return (
      <span>
        <Button onClick={this.openModal}
                bsStyle={this.props.create ? 'success' : 'info'}>
          {triggerButtonContent}
        </Button>
        <BootstrapModalForm ref="modal"
                            title={`${this.props.create ? 'Add new' : 'Edit'} connection`}
                            onSubmitForm={this._save}
                            submitButtonText="Save">
          <fieldset>
            {streamSelector}
            <Input label="Pipeline connections"
                   help={pipelineHelp}>
              <SelectableList options={this._getFilteredFormattedOptions(this.state.pipelines)}
                              isLoading={!this.state.pipelines}
                              onChange={this._onConnectionsChange}
                              selectedOptionsType="object"
                              selectedOptions={this.state.connection.pipelines} />
            </Input>
          </fieldset>
        </BootstrapModalForm>
      </span>
    );
  },
});

export default ConnectionForm;
