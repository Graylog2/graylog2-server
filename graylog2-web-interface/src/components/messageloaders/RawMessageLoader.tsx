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

import React, { useState } from 'react';
import * as Immutable from 'immutable';
import type { Subtract } from 'utility-types';
import { useTranslation } from 'react-i18next';

import { Messages } from '@graylog/server-api';

import useProductName from 'brand-customization/useProductName';
import { getValueFromInput } from 'util/FormsUtils';
import { Select, FormSubmit } from 'components/common';
import { Col, Row, Input } from 'components/bootstrap';
import { BooleanField, DropdownField, NumberField, TextField } from 'components/configurationforms';
import type { ConfigurationFieldValue } from 'components/configurationforms';
import type { Message } from 'views/components/messagelist/Types';
import useForwarderMessageLoaders from 'components/messageloaders/useForwarderMessageLoaders';
import AppConfig from 'util/AppConfig';
import useInputsList, { inputsAsMap } from 'hooks/useInputs';
import useHistory from 'routing/useHistory';
import MessageFormatter from 'logic/message/MessageFormatter';
import UserNotification from 'util/UserNotification';

import type { Input as InputType } from './Types';
import useCodecTypes from './useCodecTypes';

const DEFAULT_REMOTE_ADDRESS = '127.0.0.1';

type InputSelectProps = {
  inputs: Immutable.Map<string, InputType>;
  selectedInputId: string | undefined;
  onInputSelect: (selectedInputId: string) => void;
  show: boolean;
};

const ServerInputSelect = ({ inputs, selectedInputId, onInputSelect }: Subtract<InputSelectProps, { show }>) => {
  const { t } = useTranslation('messageloaders');
  const _formatInputSelectOptions = () => {
    if (!inputs) {
      return [{ value: 'none', label: t('loading_inputs'), disabled: true }];
    }

    if (inputs.size === 0) {
      return [{ value: 'none', label: t('no_inputs_available') }];
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
    <Input
      id="inputSelect"
      name="inputSelect"
      label={
        <>
          {t('message_input_label')} <small>{t('message_input_optional')}</small>
        </>
      }
      help={t('message_input_help')}>
      <Select
        inputId="inputSelect"
        name="inputSelect"
        aria-label={t('message_input_aria_label')}
        placeholder={t('select_input_placeholder')}
        options={_formatInputSelectOptions()}
        onChange={onInputSelect}
        value={selectedInputId}
      />
    </Input>
  );
};

const ForwarderInputSelect = ({ onInputSelect }: Pick<InputSelectProps, 'onInputSelect'>) => {
  const { t } = useTranslation('messageloaders');
  const { ForwarderInputDropdown } = useForwarderMessageLoaders();

  return (
    <>
      <ForwarderInputDropdown
        onLoadMessage={onInputSelect}
        label={`${t('forwarder_input_selection_label')} ${t('forwarder_input_selection_optional')}`}
        autoLoadMessage
      />
      <p className="description">{t('forwarder_input_selection_description')}</p>
    </>
  );
};

const InputSelect = ({ inputs, selectedInputId, onInputSelect, show }: InputSelectProps) => {
  const { t } = useTranslation('messageloaders');
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
      <legend>{`${t('input_selection_legend')} ${t('input_selection_optional')}`}</legend>
      <Input
        id="inputTypeSelect"
        type="select"
        label={`${t('select_input_type_label')} ${t('select_input_type_optional')}`}
        help={t('select_input_type_help')}
        value={selectedInputType ?? 'placeholder'}
        onChange={(e) => setSelectedInputType(e.target.value as 'server' | 'forwarder')}>
        <option value="placeholder" disabled>
          {t('select_input_type_placeholder')}
        </option>
        <option value="server">{t('server_input')}</option>
        <option value="forwarder">{t('forwarder_input')}</option>
      </Input>

      {selectedInputType === 'server' && (
        <ServerInputSelect inputs={inputs} selectedInputId={selectedInputId} onInputSelect={onInputSelect} />
      )}
      {selectedInputType === 'forwarder' && <ForwarderInputSelect onInputSelect={onInputSelect} />}
    </fieldset>
  ) : (
    <ServerInputSelect inputs={inputs} selectedInputId={selectedInputId} onInputSelect={onInputSelect} />
  );
};

type OptionsType = {
  message: string;
  remoteAddress: string;
  codec: string;
  codecConfiguration: {
    [key: string]: string;
  };
  inputId?: string;
};

type Props = {
  inputs?: Immutable.Map<string, InputType>;
  onMessageLoaded: (message: Message | undefined, option: OptionsType) => void;
  inputIdSelector?: boolean;
};

const parseRawMessage = (
  message: string,
  remoteAddress: string,
  codec: string,
  codecConfiguration: { [key: string]: {} },
  t: any,
) => {
  const payload = {
    message,
    remote_address: remoteAddress,
    codec,
    configuration: codecConfiguration,
  };

  return Messages.parse(payload).then(
    (response) => MessageFormatter.formatResultMessage(response),
    (error): undefined => {
      if (error.additional && error.additional.status === 400) {
        UserNotification.error(
          t('load_raw_message_error_description'),
          t('load_raw_message_error_title'),
        );

        return undefined;
      }

      UserNotification.error(t('load_raw_message_failed_status', { error }), t('load_raw_message_error_title'));

      return undefined;
    },
  );
};

const RawMessageLoader = ({ onMessageLoaded, inputIdSelector = false, inputs = undefined }: Props) => {
  const { t } = useTranslation('messageloaders');
  const productName = useProductName();
  const { data: codecTypes } = useCodecTypes();
  const [loading, setLoading] = useState<boolean>(false);
  const [message, setMessage] = useState<string>('');
  const [remoteAddress, setRemoteAddress] = useState<string>(DEFAULT_REMOTE_ADDRESS);
  const [codec, setCodec] = useState<string>('');
  const [codecConfiguration, setCodecConfiguration] = useState({});
  const [inputId, setInputId] = useState<string | typeof undefined>();
  const history = useHistory();

  const _loadMessage = (event: React.SyntheticEvent) => {
    event.preventDefault();

    setLoading(true);

    parseRawMessage(message, remoteAddress, codec, codecConfiguration, t)
      .then((loadedMessage) => {
        onMessageLoaded(loadedMessage, {
          message: message,
          remoteAddress: remoteAddress,
          codec: codec,
          codecConfiguration: codecConfiguration,
          inputId: inputId,
        });
      })
      .finally(() => setLoading(false));
  };

  const _formatSelectOptions = () => {
    if (!codecTypes) {
      return [{ value: 'none', label: t('loading_codec_types'), disabled: true }];
    }

    const codecTypesIds = Object.keys(codecTypes);

    if (codecTypesIds.length === 0) {
      return [{ value: 'none', label: t('no_codecs_available') }];
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

  const _onMessageChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setMessage(String(getValueFromInput(event.target)));
  };

  const _onRemoteAddressChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRemoteAddress(String(getValueFromInput(event.target)));
  };

  const _onCodecConfigurationChange = (field: string, value: ConfigurationFieldValue) => {
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
          <TextField
            key={elementKey}
            typeName={typeName}
            title={key}
            field={configField}
            value={value}
            onChange={_onCodecConfigurationChange}
          />
        );
      case 'number':
        return (
          <NumberField
            key={elementKey}
            typeName={typeName}
            title={key}
            field={configField}
            value={value}
            onChange={_onCodecConfigurationChange}
          />
        );
      case 'boolean':
        return (
          <BooleanField
            key={elementKey}
            typeName={typeName}
            title={key}
            field={configField}
            value={value}
            onChange={_onCodecConfigurationChange}
          />
        );
      case 'dropdown':
        return (
          <DropdownField
            key={elementKey}
            typeName={typeName}
            title={key}
            field={configField}
            value={value}
            onChange={_onCodecConfigurationChange}
          />
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
            <Input
              id="message"
              name="message"
              type="textarea"
              label={t('raw_message_label')}
              value={message}
              onChange={_onMessageChange}
              rows={3}
              required
            />
            <Input
              id="remoteAddress"
              name="remoteAddress"
              type="text"
              label={
                <span>
                  {t('source_ip_address_label')} <small>{t('source_ip_address_optional')}</small>
                </span>
              }
              help={t('source_ip_address_help', { productName, defaultRemoteAddress: DEFAULT_REMOTE_ADDRESS })}
              value={remoteAddress}
              onChange={_onRemoteAddressChange}
            />
          </fieldset>
          <InputSelect
            inputs={inputs}
            selectedInputId={inputId}
            onInputSelect={_onInputSelect}
            show={inputIdSelector}
          />
          <fieldset>
            <legend>{t('codec_configuration_legend')}</legend>
            <Input
              id="codec"
              name="codec"
              label={t('message_codec_label')}
              help={t('message_codec_help')}
              required>
              <Select
                id="codec"
                aria-label={t('message_codec_label')}
                placeholder={t('select_codec_placeholder')}
                options={_formatSelectOptions()}
                onChange={_onCodecSelect}
                value={codec}
              />
            </Input>
            {codecConfigurationOptions}
          </fieldset>
          <FormSubmit
            submitButtonText={t('load_message_button')}
            submitLoadingText={t('loading_message_button')}
            isSubmitting={loading}
            isAsyncSubmit
            disabledSubmit={_isSubmitDisabled}
            onCancel={() => history.goBack()}
          />
        </form>
      </Col>
    </Row>
  );
};

const RawMessageLoaderWrapper = (props: Omit<Props, 'inputs'>) => {
  const { data: inputsList } = useInputsList();
  const inputs = inputsList ? Immutable.Map(inputsAsMap(inputsList)) : undefined;

  return <RawMessageLoader {...props} inputs={inputs} />;
};

export default RawMessageLoaderWrapper;
