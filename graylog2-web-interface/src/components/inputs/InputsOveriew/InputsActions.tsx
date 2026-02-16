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

import { Button, ButtonToolbar, DeleteMenuItem, MenuItem } from 'components/bootstrap';
import { ConfirmDialog, IfPermitted } from 'components/common';
import Routes from 'routing/Routes';
import HideOnCloud from 'util/conditional/HideOnCloud';
import { isInputInSetupMode, isInputRunning } from 'components/inputs/helpers/inputState';
import recentMessagesTimeRange from 'util/TimeRangeHelper';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import useFeature from 'hooks/useFeature';
import type { ConfiguredInput, Input } from 'components/messageloaders/Types';
import InputStatesStore from 'stores/inputs/InputStatesStore';
import { LinkContainer } from 'components/common/router';
import { MoreActions } from 'components/common/EntityDataTable';
import type { InputTypesSummary } from 'hooks/useInputTypes';
import type { InputTypeDescriptionsResponse } from 'hooks/useInputTypesDescriptions';
import useInputsStates from 'hooks/useInputsStates';
import useInputMutations from 'hooks/useInputMutations';
import { INPUT_SETUP_MODE_FEATURE_FLAG, InputSetupWizard } from 'components/inputs/InputSetupWizard';
import { InputForm, InputStateControl, StaticFieldForm } from 'components/inputs';

type Props = {
  input: Input;
  inputTypes: InputTypesSummary;
  inputTypeDescriptions: InputTypeDescriptionsResponse;
  currentNode: {
    node?: {
      cluster_id: string;
      hostname: string;
      is_leader: boolean;
      is_master: boolean;
      last_seen: string;
      node_id: string;
      short_node_id: string;
      transport_address: string;
    };
  };
};

const FORWARDER_SERVICE_INPUT = 'org.graylog.plugins.forwarder.input.ForwarderServiceInput';
const GL2_FORWARDER_INPUT = 'gl2_forwarder_input';
const GL2_SOURCE_INPUT = 'gl2_source_input';

const InputsActions = ({ input, inputTypes: _, inputTypeDescriptions, currentNode }: Props) => {
  const [showConfirmDeleteDialog, setShowConfirmDeleteDialog] = useState<boolean>(false);
  const [showStaticFieldForm, setShowStaticFieldForm] = useState<boolean>(false);
  const [showConfigurationForm, setShowConfigurationForm] = useState<boolean>(false);
  const [showWizard, setShowWizard] = useState<boolean>(false);
  const { data: inputStates, isLoading: isLoadingInputStates } = useInputsStates();

  const { updateInput, deleteInput } = useInputMutations();
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const inputSetupFeatureFlagIsEnabled = useFeature(INPUT_SETUP_MODE_FEATURE_FLAG);

  const openWizard = () => {
    setShowWizard(true);
  };

  const closeWizard = () => {
    setShowWizard(false);
  };

  const onDeleteInput = () => {
    setShowConfirmDeleteDialog(true);
  };

  const editInput = () => {
    setShowConfigurationForm(true);

    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_EDIT_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'show-received-messages',
    });
  };

  const hanleInputUpdate = async (inputData: ConfiguredInput) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_UPDATED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'input-edit',
    });

    await updateInput({ input: inputData, inputId: input.id });
  };

  const enterInputSetupMode = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_SETUP_ENTERED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'input-enter-setup',
    });

    InputStatesStore.setup(input);
  };

  const exitInputSetupMode = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_SETUP_EXITED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'input-exit-setup',
    });

    InputStatesStore.stop(input);
  };

  const handleConfirmDelete = async () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_DELETED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'input-delete',
    });

    await deleteInput({ inputId: input.id });
  };

  const cancelDelete = () => {
    setShowConfirmDeleteDialog(false);
  };
  const definition = inputTypeDescriptions[input.type];
  const queryField = input.type === FORWARDER_SERVICE_INPUT ? GL2_FORWARDER_INPUT : GL2_SOURCE_INPUT;

  return (
    <ButtonToolbar>
      <IfPermitted permissions={['searches:relative']}>
        <LinkContainer
          key={`received-messages-${input.id}`}
          to={Routes.search(`${queryField}:${input.id}`, recentMessagesTimeRange())}>
          <Button
            bsSize="xsmall"
            onClick={() => {
              sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.SHOW_RECEIVED_MESSAGES_CLICKED, {
                app_pathname: getPathnameWithoutId(pathname),
                app_action_value: 'show-received-messages',
              });
            }}>
            Received messages
          </Button>
        </LinkContainer>
      </IfPermitted>

      <IfPermitted permissions={`inputs:changestate:${input.id}`}>
        {!isLoadingInputStates && (
          <InputStateControl
            key={`input-state-control-${input.id}`}
            input={input as any}
            openWizard={openWizard}
            inputStates={inputStates}
          />
        )}
      </IfPermitted>
      <MoreActions>
        <IfPermitted permissions={[`inputs:edit:${input.id}`, `input_types:create:${input.type}`]}>
          <MenuItem key={`edit-input-${input.id}`} onSelect={editInput} disabled={definition === undefined}>
            Edit input
          </MenuItem>
          <HideOnCloud>
            <IfPermitted permissions={[`inputs:edit:${input.id}`, `input_types:create:${input.type}`]}>
              <LinkContainer
                key={`manage-extractors-${input.id}`}
                to={
                  input.global
                    ? Routes.global_input_extractors(input.id)
                    : Routes.local_input_extractors(currentNode?.node?.node_id, input.id)
                }>
                <MenuItem
                  onClick={() => {
                    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.MANAGE_EXTRACTORS_CLICKED, {
                      app_pathname: getPathnameWithoutId(pathname),
                      app_action_value: 'manage-extractors',
                    });
                  }}>
                  Manage extractors
                </MenuItem>
              </LinkContainer>
            </IfPermitted>
          </HideOnCloud>
          <LinkContainer to={Routes.SYSTEM.INPUT_DIAGNOSIS(input.id)}>
            <MenuItem
              key={`input-diagnosis-${input.id}`}
              onClick={() => {
                sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_DIAGNOSIS_CLICKED, {
                  app_pathname: getPathnameWithoutId(pathname),
                  app_action_value: 'input-diagnosis',
                });
              }}>
              Input Diagnosis
            </MenuItem>
          </LinkContainer>

          {inputSetupFeatureFlagIsEnabled &&
            (isInputInSetupMode(inputStates, input.id) ? (
              <MenuItem
                key={`remove-setup-mode-${input.id}`}
                onSelect={exitInputSetupMode}
                disabled={definition === undefined}>
                Exit Setup mode
              </MenuItem>
            ) : (
              !isInputRunning(inputStates, input.id) && (
                <MenuItem
                  key={`setup-mode-${input.id}`}
                  onSelect={enterInputSetupMode}
                  disabled={definition === undefined}>
                  Enter Setup mode
                </MenuItem>
              )
            ))}
        </IfPermitted>

        {input.global && input.node && (
          <LinkContainer to={Routes.filtered_metrics(input.node, input.id)}>
            <MenuItem
              key={`show-metrics-${input.id}`}
              onClick={() => {
                sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.SHOW_METRICS_CLICKED, {
                  app_pathname: getPathnameWithoutId(pathname),
                  app_action_value: 'show-metrics',
                });
              }}>
              Show metrics
            </MenuItem>
          </LinkContainer>
        )}

        <IfPermitted permissions={[`inputs:edit:${input.id}`, `input_types:create:${input.type}`]}>
          <MenuItem
            key={`add-static-field-${input.id}`}
            onSelect={() => {
              setShowStaticFieldForm(true);
            }}>
            Add static field
          </MenuItem>
        </IfPermitted>

        <IfPermitted permissions={['inputs:terminate', `input_types:create:${input.type}`]}>
          <MenuItem key={`divider-${input.id}`} divider />
        </IfPermitted>
        <IfPermitted permissions={['inputs:terminate', `input_types:create:${input.type}`]}>
          <DeleteMenuItem key={`delete-input-${input.id}`} onSelect={onDeleteInput}>
            Delete input
          </DeleteMenuItem>
        </IfPermitted>
      </MoreActions>
      {showConfirmDeleteDialog && (
        <ConfirmDialog title="Deleting Input" show onConfirm={handleConfirmDelete} onCancel={cancelDelete}>
          Do you really want to delete input {input.title}?
        </ConfirmDialog>
      )}
      {inputSetupFeatureFlagIsEnabled && showWizard && (
        <InputSetupWizard input={input} show={showWizard} onClose={closeWizard} />
      )}
      {showStaticFieldForm && <StaticFieldForm input={input} setShowModal={setShowStaticFieldForm} />}
      {definition && showConfigurationForm && (
        <InputForm
          setShowModal={setShowConfigurationForm}
          key={`edit-form-input-${input.id}`}
          globalValue={input.global}
          nodeValue={input.node}
          configFields={definition.requested_configuration as unknown as any}
          description={definition?.description}
          title={`Editing Input ${input.title}`}
          titleValue={input.title}
          typeName={input.type}
          includeTitleField
          handleSubmit={hanleInputUpdate}
          submitButtonText="Update input"
          values={input.attributes}
        />
      )}
    </ButtonToolbar>
  );
};

export default InputsActions;
