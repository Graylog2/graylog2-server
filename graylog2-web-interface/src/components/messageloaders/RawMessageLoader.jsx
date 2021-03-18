// @flow strict
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
import { PluginStore } from 'graylog-web-plugin/plugin';
import * as Immutable from 'immutable';

import { getValueFromInput } from 'util/FormsUtils';
import { Col, Row, Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import { Select } from 'components/common';
import { BooleanField, DropdownField, NumberField, TextField } from 'components/configurationforms';
import CombinedProvider from 'injection/CombinedProvider';
import AppConfig from 'util/AppConfig';
import connect from 'stores/connect';
import type { Message } from 'views/components/messagelist/Types';

import type { Input as InputType, CodecTypes } from './Types';

const { MessagesActions } = CombinedProvider.get('Messages');
const { CodecTypesActions, CodecTypesStore } = CombinedProvider.get('CodecTypes');
const { InputsActions, InputsStore } = CombinedProvider.get('Inputs');
const isCloud = AppConfig.isCloud();
const ForwarderInputDropdown = isCloud ? PluginStore.exports('cloud')[0].messageLoaders.ForwarderInputDropdown : null;
const DEFAULT_REMOTE_ADDRESS = '127.0.0.1';

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
  onMessageLoaded: (message: ?Message, option: OptionsType) => void,
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

  const _loadMessage = (event: SyntheticEvent<*>) => {
    event.preventDefault();

    setLoading(true);
    const promise = MessagesActions.loadRawMessage(message, remoteAddress,
      codec, codecConfiguration);

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

  const _formatInputSelectOptions = () => {
    if (!inputs) {
      return [{ value: 'none', label: 'Loading inputs...', disabled: true }];
    }

    const inputIds = Object.keys(inputs);

    if (inputIds.length === 0) {
      return [{ value: 'none', label: 'No inputs available' }];
    }

    return inputIds
      .map((id) => {
        const label = `${id} / ${inputs.get(id).title} / ${inputs.get(id).name}`;

        return { value: inputId, label: label };
      })
      .sort((inputA, inputB) => inputA.label.toLowerCase().localeCompare(inputB.label.toLowerCase()));
  };

  const _onCodecSelect = (selectedCodec: string) => {
    setCodec(selectedCodec);
    setCodecConfiguration({});
  };

  const _onInputSelect = (selectedInput: string) => {
    setInputId(selectedInput);
  };

  const _onMessageChange = (event: SyntheticInputEvent<HTMLTextAreaElement>) => {
    setMessage(getValueFromInput(event.target));
  };

  const _onRemoteAddressChange = (event: SyntheticInputEvent<HTMLSelectElement>) => {
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

  const _getInputIdSelector = () => {
    if (inputIdSelector) {
      return (ForwarderInputDropdown)
        ? (
          <fieldset>
            <legend>Input selection (optional)</legend>
            <ForwarderInputDropdown onLoadMessage={_onInputSelect}
                                    autoLoadMessage />
            <p className="description">Select an Input profile from the list below then select an then select an Input.</p>
          </fieldset>
        ) : (
          <Input id="input"
                 name="input"
                 label={<span>Message input <small>(optional)</small></span>}
                 help="Select the message input ID that should be assigned to the parsed message.">
            <Select id="input"
                    placeholder="Select input"
                    options={_formatInputSelectOptions()}
                    matchProp="label"
                    onChange={_onInputSelect}
                    value={inputId} />
          </Input>
        );
    }

    return null;
  };

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
          {_getInputIdSelector()}
          <fieldset>
            <legend>Codec configuration</legend>
            <Input id="codec"
                   name="codec"
                   label="Message codec"
                   help="Select the codec that should be used to decode the message."
                   required>
              <Select id="codec"
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
  inputs: PropTypes.arrayOf(PropTypes.object),
};

RawMessageLoader.defaultProps = {
  inputIdSelector: false,
  codecTypes: undefined,
  inputs: undefined,
};

export default connect(
  RawMessageLoader,
  { inputs: InputsStore, codecTypes: CodecTypesStore },
  ({ inputs: { inputs }, codecTypes: { codecTypes } }) => ({ inputs: (inputs ? Immutable.Map(InputsStore.inputsAsMap(inputs)) : undefined), codecTypes }),
);
