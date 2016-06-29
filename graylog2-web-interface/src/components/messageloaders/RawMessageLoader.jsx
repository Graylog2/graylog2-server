import React from 'react';
import Reflux from 'reflux';
import { Button, Col, Input, Row } from 'react-bootstrap';

import { Select } from 'components/common';
import { BooleanField, DropdownField, NumberField, TextField } from 'components/configurationforms';

import ActionsProvider from 'injection/ActionsProvider';
const MessagesActions = ActionsProvider.getActions('Messages');
const CodecTypesActions = ActionsProvider.getActions('CodecTypes');

import StoreProvider from 'injection/StoreProvider';
// eslint-disable-next-line no-unused-vars
const MessagesStore = StoreProvider.getStore('Messages');
const CodecTypesStore = StoreProvider.getStore('CodecTypes');

const RawMessageLoader = React.createClass({
  mixins: [Reflux.connect(CodecTypesStore)],

  getInitialState() {
    return {
      message: '',
      remoteAddress: '',
      codec: '',
      codecConfiguration: {},
    };
  },

  componentDidMount() {
    CodecTypesActions.list();
  },

  _loadMessage(event) {
    event.preventDefault();

    const { message, remoteAddress, codec, codecConfiguration } = this.state;
    const promise = MessagesActions.loadRawMessage.triggerPromise(message, remoteAddress, codec, codecConfiguration);
    promise.then(loadedMessage => console.log('loaded: ', loadedMessage));
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
      .filter(id => id !== 'random-http-msg')
      .map(id => {
        return { value: id, label: this.state.codecTypes[id].name };
      })
      .sort((codecA, codecB) => codecA.label.toLowerCase().localeCompare(codecB.label.toLowerCase()));
  },

  _onCodecSelect(selectedCodec) {
    this._bindValue({ target: { name: 'codec', value: selectedCodec } });
    this.setState({ codecConfiguration: {} });
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
        return (<TextField key={elementKey} typeName={typeName} title={key} field={configField}
                           value={value} onChange={this._onCodecConfigurationChange} />);
      case 'number':
        return (<NumberField key={elementKey} typeName={typeName} title={key} field={configField}
                             value={value} onChange={this._onCodecConfigurationChange} />);
      case 'boolean':
        return (<BooleanField key={elementKey} typeName={typeName} title={key} field={configField}
                              value={value} onChange={this._onCodecConfigurationChange} />);
      case 'dropdown':
        return (<DropdownField key={elementKey} typeName={typeName} title={key} field={configField}
                               value={value} onChange={this._onCodecConfigurationChange} />);
      default:
        return null;
    }
  },

  render() {
    let codecConfigurationOptions;
    if (this.state.codecTypes && this.state.codec) {
      const codecConfiguration = this.state.codecTypes[this.state.codec].requested_configuration;
      codecConfigurationOptions = Object.keys(codecConfiguration)
        .sort((keyA, keyB) => codecConfiguration[keyA].is_optional - codecConfiguration[keyB].is_optional)
        .map(key => this._formatConfigField(key, codecConfiguration[key]));
    }

    return (
      <Row>
        <Col md={7}>
          <form onSubmit={this._loadMessage}>
            <fieldset>
              <Input id="message" name="message" type="textarea" label="Raw message"
                     value={this.state.message} onChange={this._bindValue} rows={3} />
              <Input id="remoteAddress" name="remoteAddress" type="text" label="Source address"
                     help="Remote address to use as message source."
                     value={this.state.remoteAddress} onChange={this._bindValue} />
            </fieldset>
            <fieldset>
              <legend>Codec configuration</legend>
              <Input id="codec" name="codec" label="Message codec"
                     help="Select the codec that should be used to decode the message.">
                <Select id="codec" placeholder="Select codec" options={this._formatSelectOptions()}
                        matchProp="label" onValueChange={this._onCodecSelect} value={this.state.codec} />
              </Input>
              {codecConfigurationOptions}
            </fieldset>
            <Button type="submit" bsStyle="info">Load raw message</Button>
          </form>
        </Col>
      </Row>
    );
  },
});

export default RawMessageLoader;
