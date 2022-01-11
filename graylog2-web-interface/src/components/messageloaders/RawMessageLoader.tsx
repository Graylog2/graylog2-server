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

import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import type { Subtract } from 'utility-types';

import { getValueFromInput } from 'util/FormsUtils';
import { Select } from 'components/common';
import { Col, Row, Button, Input } from 'components/bootstrap';
import { BooleanField, DropdownField, NumberField, TextField } from 'components/configurationforms';
import connect from 'stores/connect';
import type { Message } from 'views/components/messagelist/Types';
import useForwarderMessageLoaders from 'components/messageloaders/useForwarderMessageLoaders';
import AppConfig from 'util/AppConfig';
import { CodecTypesStore, CodecTypesActions } from 'stores/codecs/CodecTypesStore';
import { InputsActions, InputsStore } from 'stores/inputs/InputsStore';
import { MessagesActions } from 'stores/messages/MessagesStore';

import type { Input as InputType, CodecTypes } from './Types';

const DEFAULT_REMOTE_ADDRESS = '127.0.0.1';

type InputSelectProps = {
  inputs: Immutable.Map<string, InputType>,
  selectedInputId: string | undefined,
  onInputSelect: (selectedInputId: string) => void,
  show: boolean,
};

const ServerInputSelect = ({ inputs, selectedInputId, onInputSelect }: Subtract<InputSelectProps, {show}>) => {
  const _formatInputSelectOptions = () => {
    if (!inputs) {
      return [{ value: 'none', label: 'Loading inputs...', disabled: true }];
    }

    if (inputs.size === 0) {
      return [{ value: 'none', label: 'No inputs available' }];
    }

    const formattedInputs = [];

    inputs
      .sort((inputA, inputB) => inputA.title.toLowerCase().localeCompare(inputB.title.toLowerCase()))
      .forEach((input, id) => {
        const label = `${id} / ${input.title} / ${input.name}`;

        formattedInputs.push({ value: id, label: label });
      });

    return formattedInputs;
  };

  return (
    <Input id="inputSelect"
           name="inputSelect"
           label={<>Message input <small>(optional)</small></>}
           help="Select the message input ID that should be assigned to the parsed message.">
      <Select inputId="inputSelect"
              name="inputSelect"
              aria-label="Message input"
              placeholder="Select input"
              options={_formatInputSelectOptions()}
              matchProp="label"
              onChange={onInputSelect}
              value={selectedInputId} />
    </Input>
  );
};

const ForwarderInputSelect = ({ onInputSelect }: Pick<InputSelectProps, 'onInputSelect'>) => {
  const { ForwarderInputDropdown } = useForwarderMessageLoaders();

  return (
    <>
      <ForwarderInputDropdown onLoadMessage={onInputSelect}
                              label="Forwarder Input selection (optional)"
                              autoLoadMessage />
      <p className="description">Select an Input profile from the list below then select an then select an Input.</p>
    </>
  );
};

const InputSelect = ({ inputs, selectedInputId, onInputSelect, show }: InputSelectProps) => {
  const { ForwarderInputDropdown } = useForwarderMessageLoaders();
  const [selectedInputType, setSelectedInputType] = useState<'server' | 'forwarder' | undefined>();

  if (!show) {
    return null;
  }

  if (AppConfig.isCloud()) {
    return <ForwarderInputSelect onInputSelect={onInputSelect} />;
  }

  return ForwarderInputDropdown ? (
    <fieldset>
      <legend>Input selection (optional)</legend>
      <Input id="inputTypeSelect"
             type="select"
             label="Select an Input type (optional)"
             help="Select the Input type you want to load the message from."
             value={selectedInputType ?? 'placeholder'}
             onChange={(e) => setSelectedInputType(e.target.value)}>
        <option value="placeholder" disabled>Select an Input type</option>
        <option value="server">Server Input</option>
        <option value="forwarder">Forwarder Input</option>
      </Input>

      {selectedInputType === 'server' && (
        <ServerInputSelect inputs={inputs} selectedInputId={selectedInputId} onInputSelect={onInputSelect} />
      )}
      {selectedInputType === 'forwarder' && (
        <ForwarderInputSelect onInputSelect={onInputSelect} />
      )}
    </fieldset>
  ) : (
    <ServerInputSelect inputs={inputs} selectedInputId={selectedInputId} onInputSelect={onInputSelect} />
  );
};

type OptionsType = {
  message: string,
  remoteAddress: string,
  codec: string,
  codecConfiguration: {
    [key: string]: string,
  },
  inputId?: string,
};

type Props = {
  inputs?: Immutable.Map<string, InputType>,
  codecTypes: CodecTypes,
  onMessageLoaded: (message: Message | undefined, option: OptionsType) => void,
  inputIdSelector?: boolean,
};

const RawMessageLoader = ({ onMessageLoaded, inputIdSelector, codecTypes, inputs }: Props) => {
  const [loading, setLoading] = useState<boolean>(false);
  const [message, setMessage] = useState<string>('');
  const [remoteAddress, setRemoteAddress] = useState<string>(DEFAULT_REMOTE_ADDRESS);
  const [codec, setCodec] = useState<string>('');
  const [codecConfiguration, setCodecConfiguration] = useState({});
  const [inputId, setInputId] = useState<string | typeof undefined>();

  useEffect(() => {
    CodecTypesActions.list();
  }, []);

  useEffect(() => {
    if (inputIdSelector) {
      InputsActions.list();
    }
  }, [inputIdSelector]);

  const _loadMessage = (event: React.SyntheticEvent) => {
    event.preventDefault();

    setLoading(true);
    const promise = MessagesActions.loadRawMessage(message, remoteAddress, codec, codecConfiguration);

    promise.then((loadedMessage) => {
      onMessageLoaded(
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

    promise.finally(() => setLoading(false));
  };

  const _formatSelectOptions = () => {
    if (!codecTypes) {
      return [{ value: 'none', label: 'Loading codec types...', disabled: true }];
    }

    const codecTypesIds = Object.keys(codecTypes);

    if (codecTypesIds.length === 0) {
      return [{ value: 'none', label: 'No codecs available' }];
    }

    return codecTypesIds
      .filter((id) => id !== 'random-http-msg') // Skip Random HTTP codec, as nobody wants to enter a raw random message.
      .map((id) => {
        const { name } = codecTypes[id];

        // Add id as label on codecs not having a descriptor name
        return { value: id, label: name === '' ? id : name };
      })
      .sort((codecA, codecB) => codecA.label.toLowerCase().localeCompare(codecB.label.toLowerCase()));
  };

  const _onCodecSelect = (selectedCodec: string) => {
    setCodec(selectedCodec);
    setCodecConfiguration({});
  };

  const _onInputSelect = (selectedInput: string) => {
    setInputId(selectedInput);
  };

  const _onMessageChange = (event: React.SyntheticEvent<HTMLTextAreaElement>) => {
    setMessage(getValueFromInput(event.target));
  };

  const _onRemoteAddressChange = (event: React.SyntheticEvent<HTMLSelectElement>) => {
    setRemoteAddress(getValueFromInput(event.target));
  };

  const _onCodecConfigurationChange = (field: string, value: string) => {
    const newConfiguration = { ...codecConfiguration, [field]: value };
    setCodecConfiguration(newConfiguration);
  };

  const _formatConfigField = (key: string, configField) => {
    const value = codecConfiguration[key];
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
                     onChange={_onCodecConfigurationChange} />
        );
      case 'number':
        return (
          <NumberField key={elementKey}
                       typeName={typeName}
                       title={key}
                       field={configField}
                       value={value}
                       onChange={_onCodecConfigurationChange} />
        );
      case 'boolean':
        return (
          <BooleanField key={elementKey}
                        typeName={typeName}
                        title={key}
                        field={configField}
                        value={value}
                        onChange={_onCodecConfigurationChange} />
        );
      case 'dropdown':
        return (
          <DropdownField key={elementKey}
                         typeName={typeName}
                         title={key}
                         field={configField}
                         value={value}
                         onChange={_onCodecConfigurationChange} />
        );
      default:
        return null;
    }
  };

  const _isSubmitDisabled = !message || !codec || loading;

  let codecConfigurationOptions;

  if (codecTypes && codec) {
    const currentCodecConfiguration = codecTypes[codec].requested_configuration;

    codecConfigurationOptions = Object.keys(currentCodecConfiguration)
      .sort((keyA, keyB) => currentCodecConfiguration[keyA].is_optional - currentCodecConfiguration[keyB].is_optional)
      .map((key) => _formatConfigField(key, currentCodecConfiguration[key]));
  }

  return (
    <Row>
      <Col md={7}>
        <form onSubmit={_loadMessage}>
          <fieldset>
            <Input id="message"
                   name="message"
                   type="textarea"
                   label="Raw message"
                   value={message}
                   onChange={_onMessageChange}
                   rows={3}
                   required />
            <Input id="remoteAddress"
                   name="remoteAddress"
                   type="text"
                   label={<span>Source IP address <small>(optional)</small></span>}
                   help={`Remote IP address to use as message source. Graylog will use ${DEFAULT_REMOTE_ADDRESS} by default.`}
                   value={remoteAddress}
                   onChange={_onRemoteAddressChange} />
          </fieldset>
          <InputSelect inputs={inputs}
                       selectedInputId={inputId}
                       onInputSelect={_onInputSelect}
                       show={inputIdSelector} />
          <fieldset>
            <legend>Codec configuration</legend>
            <Input id="codec"
                   name="codec"
                   label="Message codec"
                   help="Select the codec that should be used to decode the message."
                   required>
              <Select id="codec"
                      aria-label="Message codec"
                      placeholder="Select codec"
                      options={_formatSelectOptions()}
                      matchProp="label"
                      onChange={_onCodecSelect}
                      value={codec} />
            </Input>
            {codecConfigurationOptions}
          </fieldset>
          <Button type="submit" bsStyle="info" disabled={_isSubmitDisabled}>
            {loading ? 'Loading message...' : 'Load message'}
          </Button>
        </form>
      </Col>
    </Row>
  );
};

RawMessageLoader.propTypes = {
  onMessageLoaded: PropTypes.func.isRequired,
  inputIdSelector: PropTypes.bool,
  codecTypes: PropTypes.object,
  inputs: PropTypes.object,
};

RawMessageLoader.defaultProps = {
  inputIdSelector: false,
  codecTypes: undefined,
  inputs: undefined,
};

export default connect(
  // @ts-ignore
  RawMessageLoader,
  { inputs: InputsStore, codecTypes: CodecTypesStore },
  // @ts-ignore
  ({ inputs: { inputs }, codecTypes: { codecTypes } }) => ({ inputs: (inputs ? Immutable.Map(InputsStore.inputsAsMap(inputs)) : undefined), codecTypes }),
);
