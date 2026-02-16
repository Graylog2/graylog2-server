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
import { useQueryClient } from '@tanstack/react-query';

import { InputsActions } from 'stores/inputs/InputsStore';
import type { InputDescription } from 'stores/inputs/InputTypesStore';
import { InputTypesActions } from 'stores/inputs/InputTypesStore';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { Col, Row, Button } from 'components/bootstrap';
import { Select } from 'components/common';
import { InputForm } from 'components/inputs';
import type { ConfiguredInput, Input } from 'components/messageloaders/Types';
import useInputTypes from 'components/inputs/useInputTypes';
import { KEY_PREFIX } from 'hooks/usePaginatedInputs';
import useFeature from 'hooks/useFeature';
import { INPUT_SETUP_MODE_FEATURE_FLAG, InputSetupWizard } from 'components/inputs/InputSetupWizard';

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
  const [showWizard, setShowWizard] = useState<boolean>(false);
  const [createdInputId, setCreatedInputId] = useState<string | null>(null);
  const [createdInputData, setCreatedInputData] = useState<ConfiguredInput | null>(null);
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const inputTypes = useInputTypes();
  const queryClient = useQueryClient();
  const inputSetupFeatureFlagIsEnabled = useFeature(INPUT_SETUP_MODE_FEATURE_FLAG);

  const openWizard = (inputId: string, inputData: ConfiguredInput) => {
    setCreatedInputId(inputId);
    setCreatedInputData(inputData);
    setShowWizard(true);
  };

  const closeWizard = () => {
    setShowWizard(false);
    setCreatedInputId(null);
    setCreatedInputData(null);
  };

  const resetFields = () => {
    setSelectedInput(undefined);
    setSelectedInputDefinition(undefined);
  };

  const formatSelectOptions = () => {
    let options = [];

    if (inputTypes) {
      const inputTypesIds = Object.keys(inputTypes);

      options = inputTypesIds.map((id) => ({ value: id, label: inputTypes[id] }));

      options.sort((inputTypeA, inputTypeB) =>
        inputTypeA.label.toLowerCase().localeCompare(inputTypeB.label.toLowerCase()),
      );
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

    InputTypesActions.get(selected).then((inputDefinition: InputDescription) =>
      setSelectedInputDefinition(inputDefinition),
    );
  };

  const onCustomInputClose = () => {
    setCustomInputConfiguration(undefined);
  };

  const handleInputTypeSubmit = (event) => {
    event.preventDefault();

    const customConfiguration = PluginStore.exports('inputConfiguration').find(
      (inputConfig) => inputConfig.type === selectedInput,
    );

    setCustomInputConfiguration(customConfiguration);

    setShowConfigurationForm(true);
  };

  const createInput = (data: ConfiguredInput) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_CREATED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'input-create',
    });

    InputsActions.create(data).then((response: { id: string }) => {
      queryClient.invalidateQueries({ queryKey: KEY_PREFIX });

      if (inputSetupFeatureFlagIsEnabled && response?.id) {
        setTimeout(() => openWizard(response.id, data), 500);
      }

      resetFields();
    });
  };

  const createInputForWizard = () => {
    if (!createdInputId || !createdInputData) return null;

    return {
      id: createdInputId,
      title: createdInputData.title,
      type: createdInputData.type,
      attributes: createdInputData.configuration,
      global: createdInputData.global || false,
      node: createdInputData.node || null,
    } as Input;
  };

  const CustomInputsConfiguration = customInputConfiguration ? customInputConfiguration.component : null;

  return (
    <Row className="content">
      <Col md={12}>
        <StyledForm className="form-inline" onSubmit={handleInputTypeSubmit}>
          <FormGroup>
            <Select
              placeholder="Select input"
              options={formatSelectOptions()}
              onChange={onInputSelect}
              value={selectedInput}
            />
          </FormGroup>
          &nbsp;
          <Button bsStyle="primary" type="submit" disabled={!selectedInput}>
            Launch new input
          </Button>
        </StyledForm>
        {selectedInputDefinition &&
          (customInputConfiguration ? (
            <CustomInputsConfiguration onClose={onCustomInputClose} />
          ) : (
            showConfigurationForm && (
              <InputForm
                key="configuration-form-input"
                setShowModal={setShowConfigurationForm}
                configFields={selectedInputDefinition.requested_configuration}
                description={selectedInputDefinition.description}
                title={
                  <span>
                    Launch new <em>{inputTypes[selectedInput] ?? ''}</em> input
                  </span>
                }
                submitButtonText="Launch Input"
                typeName={selectedInput}
                handleSubmit={createInput}
              />
            )
          ))}
        {inputSetupFeatureFlagIsEnabled && showWizard && createdInputId && (
          <InputSetupWizard input={createInputForWizard()} show={showWizard} onClose={closeWizard} />
        )}
      </Col>
    </Row>
  );
};

export default CreateInputControl;
