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
import Reflux from 'reflux';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Col, Row, Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import { Select } from 'components/common';
import { BooleanField, DropdownField, NumberField, TextField } from 'components/configurationforms';
import ActionsProvider from 'injection/ActionsProvider';
import StoreProvider from 'injection/StoreProvider';
import AppConfig from 'util/AppConfig';

const MessagesActions = ActionsProvider.getActions('Messages');
const CodecTypesActions = ActionsProvider.getActions('CodecTypes');
const InputsActions = ActionsProvider.getActions('Inputs');
// eslint-disable-next-line no-unused-vars
const MessagesStore = StoreProvider.getStore('Messages');
const CodecTypesStore = StoreProvider.getStore('CodecTypes');
const InputsStore = StoreProvider.getStore('Inputs');
const isCloud = AppConfig.isCloud();
const ForwarderInputDropdown = isCloud ? PluginStore.exports('cloud')[0].messageLoaders.ForwarderInputDropdown : null;

const RawMessageLoader = createReactClass({
  displayName: 'RawMessageLoader',

  propTypes: {
    onMessageLoaded: PropTypes.func.isRequired,
    inputIdSelector: PropTypes.bool,
  },

  mixins: [Reflux.connect(CodecTypesStore), Reflux.connect(InputsStore)],

  getDefaultProps() {
    return {
      inputIdSelector: false,
    };
  },

  getInitialState() {
    return {
      loading: false,
      message: '',
      remoteAddress: '',
      codec: '',
      codecConfiguration: {},
      inputId: undefined,
    };
  },

  componentDidMount() {
    CodecTypesActions.list();

    if (this.props.inputIdSelector) {
      InputsActions.list();
    }
  },

  DEFAULT_REMOTE_ADDRESS: '127.0.0.1',

  _loadMessage(event) {
    event.preventDefault();

    const { message, remoteAddress, codec, codecConfiguration, inputId } = this.state;

    this.setState({ loading: true });
    const promise = MessagesActions.loadRawMessage.triggerPromise(message, remoteAddress || this.DEFAULT_REMOTE_ADDRESS,
      codec, codecConfiguration);

    promise.then((loadedMessage) => {
      this.props.onMessageLoaded(
        loadedMessage,
        {
          message: message,
          remoteAddress: remoteAddress,
          codec: codec,
          codecConfiguration: codecConfiguration,
          inputId: inputId,
        },
      );
    });

    promise.finally(() => this.setState({ loading: false }));
  },

  _bindValue(event) {
    const newState = {};

    newState[event.target.name] = event.target.value;
    this.setState(newState);
  },

  _formatSelectOptions() {
    if (!this.state.codecTypes) {
      return [{ value: 'none', label: 'Loading codec types...', disabled: true }];
    }

    const codecTypesIds = Object.keys(this.state.codecTypes);

    if (codecTypesIds.length === 0) {
      return [{ value: 'none', label: 'No codecs available' }];
    }

    return codecTypesIds
      .filter((id) => id !== 'random-http-msg') // Skip Random HTTP codec, as nobody wants to enter a raw random message.
      .map((id) => {
        const { name } = this.state.codecTypes[id];

        // Add id as label on codecs not having a descriptor name
        return { value: id, label: name === '' ? id : name };
      })
      .sort((codecA, codecB) => codecA.label.toLowerCase().localeCompare(codecB.label.toLowerCase()));
  },

  _formatInputSelectOptions() {
    if (!this.state.inputs) {
      return [{ value: 'none', label: 'Loading inputs...', disabled: true }];
    }

    const inputIds = Object.keys(this.state.inputs);

    if (inputIds.length === 0) {
      return [{ value: 'none', label: 'No inputs available' }];
    }

    return inputIds
      .map((id) => {
        const inputId = this.state.inputs[id].id;
        const label = `${inputId} / ${this.state.inputs[id].title} / ${this.state.inputs[id].name}`;

        return { value: inputId, label: label };
      })
      .sort((inputA, inputB) => inputA.label.toLowerCase().localeCompare(inputB.label.toLowerCase()));
  },

  _onCodecSelect(selectedCodec) {
    this._bindValue({ target: { name: 'codec', value: selectedCodec } });
    this.setState({ codecConfiguration: {} });
  },

  _onInputSelect(selectedInput) {
    this.setState({ inputId: selectedInput });
  },

  _onCodecConfigurationChange(field, value) {
    const newConfiguration = Object.assign(this.state.codecConfiguration);

    newConfiguration[field] = value;
    this._bindValue({ target: { name: 'codecConfiguration', value: newConfiguration } });
  },

  _formatConfigField(key, configField) {
    const value = this.state.codecConfiguration[key];
    const typeName = 'RawMessageLoader';
    const elementKey = `${typeName}-${key}`;

    switch (configField.type) {
      case 'text':
        return (
          <TextField key={elementKey}
                     typeName={typeName}
                     title={key}
                     field={configField}
                     value={value}
                     onChange={this._onCodecConfigurationChange} />
        );
      case 'number':
        return (
          <NumberField key={elementKey}
                       typeName={typeName}
                       title={key}
                       field={configField}
                       value={value}
                       onChange={this._onCodecConfigurationChange} />
        );
      case 'boolean':
        return (
          <BooleanField key={elementKey}
                        typeName={typeName}
                        title={key}
                        field={configField}
                        value={value}
                        onChange={this._onCodecConfigurationChange} />
        );
      case 'dropdown':
        return (
          <DropdownField key={elementKey}
                         typeName={typeName}
                         title={key}
                         field={configField}
                         value={value}
                         onChange={this._onCodecConfigurationChange} />
        );
      default:
        return null;
    }
  },

  _isSubmitDisabled() {
    return !this.state.message || !this.state.codec || this.state.loading;
  },

  _getInputIdSelector() {
    if (this.props.inputIdSelector) {
      const inputIdSelector = (isCloud && ForwarderInputDropdown)
        ? (

          <fieldset>
            <legend>Input selection (optional)</legend>
            <ForwarderInputDropdown onLoadMessage={this._onInputSelect}
                                    rawMessageLoader />
          </fieldset>
        )
        : (
          <Input id="input"
                 name="input"
                 label={<span>Message input <small>(optional)</small></span>}
                 help="Select the message input ID that should be assigned to the parsed message.">
            <Select id="input"
                    placeholder="Select input"
                    options={this._formatInputSelectOptions()}
                    matchProp="label"
                    onChange={this._onInputSelect}
                    value={this.state.inputId} />
          </Input>
        );

      return inputIdSelector;
    }

    return null;
  },

  render() {
    let codecConfigurationOptions;

    if (this.state.codecTypes && this.state.codec) {
      const codecConfiguration = this.state.codecTypes[this.state.codec].requested_configuration;

      codecConfigurationOptions = Object.keys(codecConfiguration)
        .sort((keyA, keyB) => codecConfiguration[keyA].is_optional - codecConfiguration[keyB].is_optional)
        .map((key) => this._formatConfigField(key, codecConfiguration[key]));
    }

    // let inputIdSelector;

    return (
      <Row>
        <Col md={7}>
          <form onSubmit={this._loadMessage}>
            <fieldset>
              <Input id="message"
                     name="message"
                     type="textarea"
                     label="Raw message"
                     value={this.state.message}
                     onChange={this._bindValue}
                     rows={3}
                     required />
              <Input id="remoteAddress"
                     name="remoteAddress"
                     type="text"
                     label={<span>Source IP address <small>(optional)</small></span>}
                     help={`Remote IP address to use as message source. Graylog will use ${this.DEFAULT_REMOTE_ADDRESS} by default.`}
                     value={this.state.remoteAddress}
                     onChange={this._bindValue} />
            </fieldset>
            {this._getInputIdSelector()}
            <fieldset>
              <legend>Codec configuration</legend>
              <Input id="codec"
                     name="codec"
                     label="Message codec"
                     help="Select the codec that should be used to decode the message."
                     required>
                <Select id="codec"
                        placeholder="Select codec"
                        options={this._formatSelectOptions()}
                        matchProp="label"
                        onChange={this._onCodecSelect}
                        value={this.state.codec} />
              </Input>
              {codecConfigurationOptions}
            </fieldset>
            <Button type="submit" bsStyle="info" disabled={this._isSubmitDisabled()}>
              {this.state.loading ? 'Loading message...' : 'Load message'}
            </Button>
          </form>
        </Col>
      </Row>
    );
  },
});

export default RawMessageLoader;
