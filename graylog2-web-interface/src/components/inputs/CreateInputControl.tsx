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

import * as React from 'react';
import { useState } from 'react';
import styled from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { useStore } from 'stores/connect';
import { InputsActions } from 'stores/inputs/InputsStore';
import { InputTypesActions, InputTypesStore } from 'stores/inputs/InputTypesStore';
import type { InputDescription } from 'stores/inputs/InputTypesStore';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { Col, Row, Button } from 'components/bootstrap';
import { ExternalLinkButton, Select } from 'components/common';
import { InputForm } from 'components/inputs';
import type { ConfiguredInput } from 'components/messageloaders/Types';

const StyledForm = styled.form`
  display: flex;
  gap: 0.25em;
`;

const FormGroup = styled.div`
  width: 300px;
  display: inline-block;
  margin-bottom: 0;
  vertical-align: middle;
`;

const CreateInputControl = () => {
  const [showConfigurationForm, setShowConfigurationForm] = useState<boolean>(false);
  const [selectedInput, setSelectedInput] = useState<string | undefined>(undefined);
  const [selectedInputDefinition, setSelectedInputDefinition] = useState<InputDescription | undefined>(undefined);
  const [customInputConfiguration, setCustomInputConfiguration] = useState(undefined);
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const { inputTypes } = useStore(InputTypesStore);

  const resetFields = () => {
    setSelectedInput(undefined);
    setSelectedInputDefinition(undefined);
  };

  const formatSelectOptions = () => {
    let options = [];

    if (inputTypes) {
      const inputTypesIds = Object.keys(inputTypes);

      options = inputTypesIds.map((id) => ({ value: id, label: inputTypes[id] }));

      options.sort((inputTypeA, inputTypeB) => inputTypeA.label.toLowerCase().localeCompare(inputTypeB.label.toLowerCase()));
    } else {
      options.push({ value: 'none', label: 'No inputs available', disabled: true });
    }

    return options;
  };

  const onInputSelect = (selected: string) => {
    if (selected === '') {
      resetFields();
    }

    setSelectedInput(selected);

    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_SELECTED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'input-select',
      event_details: { value: selected },
    });

    InputTypesActions.get.triggerPromise(selected).then((inputDefinition: InputDescription) => setSelectedInputDefinition(inputDefinition));
  };

  const onCustomInputClose = () => {
    setCustomInputConfiguration(undefined);
  };

  const handleInputTypeSubmit = (event) => {
    event.preventDefault();

    const customConfiguration = PluginStore.exports('inputConfiguration')
      .find((inputConfig) => inputConfig.type === selectedInput);

    setCustomInputConfiguration(customConfiguration);

    setShowConfigurationForm(true);
  };

  const createInput = (data: ConfiguredInput) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_CREATED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'input-create',
    });

    InputsActions.create(data).then(() => {
      resetFields();
    });
  };

  const handleMarketplaceClick = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.FIND_MORE_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'inputs-find-more',
    });
  };

  const CustomInputsConfiguration = customInputConfiguration ? customInputConfiguration.component : null;

  return (
    <Row className="content">
      <Col md={12}>
        <StyledForm className="form-inline" onSubmit={handleInputTypeSubmit}>
          <FormGroup>
            <Select placeholder="Select input"
                    options={formatSelectOptions()}
                    matchProp="label"
                    onChange={onInputSelect}
                    value={selectedInput} />
          </FormGroup>
            &nbsp;
          <Button bsStyle="success" type="submit" disabled={!selectedInput}>Launch new input</Button>
          <ExternalLinkButton href="https://marketplace.graylog.org/"
                              bsStyle="info"
                              onClick={handleMarketplaceClick}>
            Find more inputs
          </ExternalLinkButton>
        </StyledForm>
        {selectedInputDefinition && (
          customInputConfiguration ? (
            <CustomInputsConfiguration onClose={onCustomInputClose} />
          ) : (
            showConfigurationForm && (
            <InputForm key="configuration-form-input"
                       setShowModal={setShowConfigurationForm}
                       configFields={selectedInputDefinition.requested_configuration}
                       title={<span>Launch new <em>{inputTypes[selectedInput] ?? ''}</em> input</span>}
                       submitButtonText="Launch Input"
                       helpBlock="Select a name of your new input that describes it."
                       typeName={selectedInput}
                       handleSubmit={createInput} />
            )
          )
        )}
      </Col>
    </Row>
  );
};

export default CreateInputControl;
