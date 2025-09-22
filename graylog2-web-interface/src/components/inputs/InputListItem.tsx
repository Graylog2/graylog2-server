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

import type { Input, ConfiguredInput } from 'components/messageloaders/Types';
import { useStore } from 'stores/connect';
import { LinkContainer } from 'components/common/router';
import { DropdownButton, MenuItem, Col, Button, DeleteMenuItem } from 'components/bootstrap';
import { ConfirmDialog, EntityListItem, IfPermitted, LinkToNode, Spinner } from 'components/common';
import { ConfigurationWell } from 'components/configurationforms';
import Routes from 'routing/Routes';
import recentMessagesTimeRange from 'util/TimeRangeHelper';
import {
  InputForm,
  InputStateBadge,
  InputStateControl,
  InputStaticFields,
  InputThroughput,
  StaticFieldForm,
} from 'components/inputs';
import { InputsActions } from 'stores/inputs/InputsStore';
import { InputTypesStore } from 'stores/inputs/InputTypesStore';
import { InputStatesStore } from 'stores/inputs/InputStatesStore';
import type { InputStates } from 'stores/inputs/InputStatesStore';
import { isInputInSetupMode, isInputRunning } from 'components/inputs/helpers/inputState';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import useFeature from 'hooks/useFeature';
import { INPUT_SETUP_MODE_FEATURE_FLAG, InputSetupWizard } from 'components/inputs/InputSetupWizard';
import HideOnCloud from 'util/conditional/HideOnCloud';

type Props = {
  input: Input;
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

const InputListItem = ({ input, currentNode }: Props) => {
  const [showConfirmDeleteDialog, setShowConfirmDeleteDialog] = useState<boolean>(false);
  const [showStaticFieldForm, setShowStaticFieldForm] = useState<boolean>(false);
  const [showConfigurationForm, setShowConfigurationForm] = useState<boolean>(false);
  const [showWizard, setShowWizard] = useState<boolean>(false);
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const { inputTypes, inputDescriptions } = useStore(InputTypesStore);
  const { inputStates } = useStore(InputStatesStore) as { inputStates: InputStates };
  const inputSetupFeatureFlagIsEnabled = useFeature(INPUT_SETUP_MODE_FEATURE_FLAG);

  const openWizard = () => {
    setShowWizard(true);
  };

  const closeWizard = () => {
    setShowWizard(false);
  };

  const deleteInput = () => {
    setShowConfirmDeleteDialog(true);
  };

  const editInput = () => {
    setShowConfigurationForm(true);

    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_EDIT_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'show-received-messages',
    });
  };

  const updateInput = (inputData: ConfiguredInput) => {
    InputsActions.update(input.id, inputData);

    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_UPDATED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'input-edit',
    });
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

  const handleConfirmDelete = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_DELETED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'input-delete',
    });

    InputsActions.delete(input);
  };

  const cancelDelete = () => {
    setShowConfirmDeleteDialog(false);
  };

  if (!inputTypes) {
    return <Spinner />;
  }

  const definition = inputDescriptions[input.type];

  const titleSuffix = (
    <span>
      {input.name}
      &nbsp; ({input.id}) &nbsp;
      <InputStateBadge input={input} />
    </span>
  );

  const queryField =
    input.type === 'org.graylog.plugins.forwarder.input.ForwarderServiceInput'
      ? 'gl2_forwarder_input'
      : 'gl2_source_input';

  const actions = [
    <IfPermitted permissions={['searches:relative']}>
      <LinkContainer
        key={`received-messages-${input.id}`}
        to={Routes.search(`${queryField}:${input.id}`, recentMessagesTimeRange())}>
        <Button
          onClick={() => {
            sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.SHOW_RECEIVED_MESSAGES_CLICKED, {
              app_pathname: getPathnameWithoutId(pathname),
              app_action_value: 'show-received-messages',
            });
          }}>
          Show received messages
        </Button>
      </LinkContainer>
    </IfPermitted>,
    <HideOnCloud>
      <IfPermitted permissions={[`inputs:edit:${input.id}`, `input_types:create:${input.type}`]}>
        <LinkContainer
          key={`manage-extractors-${input.id}`}
          to={
            input.global
              ? Routes.global_input_extractors(input.id)
              : Routes.local_input_extractors(currentNode?.node?.node_id, input.id)
          }>
          <Button
            onClick={() => {
              sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.MANAGE_EXTRACTORS_CLICKED, {
                app_pathname: getPathnameWithoutId(pathname),
                app_action_value: 'manage-extractors',
              });
            }}>
            Manage extractors
          </Button>
        </LinkContainer>
      </IfPermitted>
    </HideOnCloud>,
    <IfPermitted permissions={[`inputs:edit:${input.id}`, `input_types:create:${input.type}`]}>
      <InputStateControl key={`input-state-control-${input.id}`} input={input} openWizard={openWizard} />
    </IfPermitted>,
    <DropdownButton
      key={`more-actions-${input.id}`}
      title="More actions"
      id={`more-actions-dropdown-${input.id}`}
      pullRight>
      <IfPermitted permissions={[`inputs:edit:${input.id}`, `input_types:create:${input.type}`]}>
        <MenuItem key={`edit-input-${input.id}`} onSelect={editInput} disabled={definition === undefined}>
          Edit input
        </MenuItem>

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
        <DeleteMenuItem key={`delete-input-${input.id}`} onSelect={deleteInput}>
          Delete input
        </DeleteMenuItem>
      </IfPermitted>
    </DropdownButton>,
  ] as const;

  const subtitle = () => {
    if (input.global && !input.node) return null;

    return (
      <span>
        On node <LinkToNode nodeId={input.node} />
      </span>
    );
  };

  const additionalContent = (
    <div>
      {inputSetupFeatureFlagIsEnabled && showWizard && (
        <InputSetupWizard input={input} show={showWizard} onClose={closeWizard} />
      )}
      <Col md={8}>
        <ConfigurationWell id={input.id} configuration={input.attributes} typeDefinition={definition} />
        {showStaticFieldForm && <StaticFieldForm input={input} setShowModal={setShowStaticFieldForm} />}

        <InputStaticFields input={input} />
      </Col>
      <Col md={4}>
        <InputThroughput input={input} />
      </Col>
      {definition && showConfigurationForm && (
        <InputForm
          setShowModal={setShowConfigurationForm}
          key={`edit-form-input-${input.id}`}
          globalValue={input.global}
          nodeValue={input.node}
          configFields={definition.requested_configuration}
          description={definition?.description}
          title={`Editing Input ${input.title}`}
          titleValue={input.title}
          typeName={input.type}
          includeTitleField
          handleSubmit={updateInput}
          submitButtonText="Update input"
          values={input.attributes}
        />
      )}
    </div>
  );

  return (
    <>
      <EntityListItem
        key={`entry-list-${input.id}`}
        title={input.title}
        titleSuffix={titleSuffix}
        description={subtitle()}
        actions={actions}
        contentRow={additionalContent}
      />
      {showConfirmDeleteDialog && (
        <ConfirmDialog title="Deleting Input" show onConfirm={handleConfirmDelete} onCancel={cancelDelete}>
          Do you really want to delete input {input.title}?
        </ConfirmDialog>
      )}
    </>
  );
};

export default InputListItem;
