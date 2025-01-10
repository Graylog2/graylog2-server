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
import AppConfig from 'util/AppConfig';
import { LinkContainer } from 'components/common/router';
import { isPermitted } from 'util/PermissionsMixin';
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

type Props = {
  input: Input,
  currentNode: {
    node?: {
      cluster_id: string,
      hostname: string,
      is_leader: boolean,
      is_master:boolean,
      last_seen: string,
      node_id: string,
      short_node_id: string
      transport_address: string,
  }
},
  permissions: Array<string>,
}

const InputListItem = ({ input, currentNode, permissions }: Props) => {
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
      &nbsp;
      ({input.id})
        &nbsp;
      <InputStateBadge input={input} />
    </span>
  );

  const actions = [];

  const queryField = (input.type === 'org.graylog.plugins.forwarder.input.ForwarderServiceInput') ? 'gl2_forwarder_input' : 'gl2_source_input';

  if (isPermitted(permissions, ['searches:relative'])) {
    actions.push(
      <LinkContainer key={`received-messages-${input.id}`}
                     to={Routes.search(`${queryField}:${input.id}`, recentMessagesTimeRange())}>
        <Button onClick={() => {
          sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.SHOW_RECEIVED_MESSAGES_CLICKED, {
            app_pathname: getPathnameWithoutId(pathname),
            app_action_value: 'show-received-messages',
          });
        }}>
          Show received messages
        </Button>
      </LinkContainer>,
    );
  }

  if (isPermitted(permissions, [`inputs:edit:${input.id}`])) {
    if (!AppConfig.isCloud()) {
      let extractorRoute;

      if (input.global) {
        extractorRoute = Routes.global_input_extractors(input.id);
      } else {
        extractorRoute = Routes.local_input_extractors(currentNode?.node?.node_id, input.id);
      }

      actions.push(
        <LinkContainer key={`manage-extractors-${input.id}`} to={extractorRoute}>
          <Button onClick={() => {
            sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.MANAGE_EXTRACTORS_CLICKED, {
              app_pathname: getPathnameWithoutId(pathname),
              app_action_value: 'manage-extractors',
            });
          }}>
            Manage extractors
          </Button>
        </LinkContainer>,
      );
    }

    actions.push(<InputStateControl key={`input-state-control-${input.id}`} input={input} openWizard={openWizard} />);
  }

  actions.push(
    <DropdownButton key={`more-actions-${input.id}`}
                    title="More actions"
                    id={`more-actions-dropdown-${input.id}`}
                    pullRight>
      <IfPermitted permissions={`inputs:edit:${input.id}`}>
        <MenuItem key={`edit-input-${input.id}`}
                  onSelect={editInput}
                  disabled={definition === undefined}>
          Edit input
        </MenuItem>
        {inputSetupFeatureFlagIsEnabled && (
          isInputInSetupMode(inputStates, input.id) ? (
            <MenuItem key={`remove-setup-mode-${input.id}`}
                      onSelect={exitInputSetupMode}
                      disabled={definition === undefined}>
              Exit Setup mode
            </MenuItem>
          ) : (
            !isInputRunning(inputStates, input.id) && (
            <MenuItem key={`setup-mode-${input.id}`}
                      onSelect={enterInputSetupMode}
                      disabled={definition === undefined}>
              Enter Setup mode
            </MenuItem>
            )
          )
        )}
      </IfPermitted>

      {input.global && (
        <LinkContainer to={Routes.filtered_metrics(input.node, input.id)}>
          <MenuItem key={`show-metrics-${input.id}`}
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

      <IfPermitted permissions={`inputs:edit:${input.id}`}>
        <MenuItem key={`add-static-field-${input.id}`} onSelect={() => { setShowStaticFieldForm(true); }}>Add static
          field
        </MenuItem>
      </IfPermitted>

      <IfPermitted permissions="inputs:terminate">
        <MenuItem key={`divider-${input.id}`} divider />
      </IfPermitted>
      <IfPermitted permissions="inputs:terminate">
        <DeleteMenuItem key={`delete-input-${input.id}`} onSelect={deleteInput}>Delete input</DeleteMenuItem>
      </IfPermitted>
    </DropdownButton>,
  );

  const subtitle = () => {
    if (input.global && !input.node) return null;

    return (
      <span>
        On node{' '}<LinkToNode nodeId={input.node} />
      </span>
    );
  };

  const additionalContent = (
    <div>
      {inputSetupFeatureFlagIsEnabled && showWizard && (<InputSetupWizard input={input} show={showWizard} onClose={closeWizard} />)}
      <Col md={8}>
        <ConfigurationWell id={input.id}
                           configuration={input.attributes}
                           typeDefinition={definition || {}} />
        {showStaticFieldForm && (
        <StaticFieldForm input={input}
                         setShowModal={setShowStaticFieldForm} />
        )}

        <InputStaticFields input={input} />
      </Col>
      <Col md={4}>
        <InputThroughput input={input} />
      </Col>
      {definition && showConfigurationForm && (
      <InputForm setShowModal={setShowConfigurationForm}
                 key={`edit-form-input-${input.id}`}
                 globalValue={input.global}
                 nodeValue={input.node}
                 configFields={definition.requested_configuration}
                 title={`Editing Input ${input.title}`}
                 titleValue={input.title}
                 typeName={input.type}
                 includeTitleField
                 handleSubmit={updateInput}
                 submitButtonText="Update input"
                 values={input.attributes} />
      )}
    </div>
  );

  return (
    <>
      <EntityListItem key={`entry-list-${input.id}`}
                      title={input.title}
                      titleSuffix={titleSuffix}
                      description={subtitle()}
                      actions={actions}
                      contentRow={additionalContent} />
      {showConfirmDeleteDialog && (
        <ConfirmDialog title="Deleting Input"
                       show
                       onConfirm={handleConfirmDelete}
                       onCancel={cancelDelete}>
          Do you really want to delete input {input.title}?
        </ConfirmDialog>
      )}
    </>
  );
};

export default InputListItem;
