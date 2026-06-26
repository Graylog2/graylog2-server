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
import styled, { css } from 'styled-components';

import { Button } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import { InputForm } from 'components/inputs';
import type { ConfiguredInput, Input } from 'components/messageloaders/Types';
import useInputMutations from 'hooks/useInputMutations';
import useInputTypesDescriptions from 'hooks/useInputTypesDescriptions';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';

const StyledButton = styled(Button)(
  ({ theme }) => css`
    margin-right: ${theme.spacings.md};
  `,
);

type Props = {
  input: Input;
};

const EditInputButton = ({ input }: Props) => {
  const [showConfigurationForm, setShowConfigurationForm] = useState<boolean>(false);
  const { data: inputTypeDescriptions } = useInputTypesDescriptions();
  const { updateInput } = useInputMutations();
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  const definition = inputTypeDescriptions?.[input.type];

  const editInput = () => {
    setShowConfigurationForm(true);

    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_EDIT_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'edit-input',
    });
  };

  const handleInputUpdate = async (inputData: ConfiguredInput) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_UPDATED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'input-edit',
    });

    await updateInput({ input: inputData, inputId: input.id });
  };

  return (
    <IfPermitted permissions={[`inputs:edit:${input.id}`, `input_types:create:${input.type}`]}>
      <StyledButton bsStyle="primary" onClick={editInput} disabled={definition === undefined}>
        Edit input
      </StyledButton>
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
          handleSubmit={handleInputUpdate}
          submitButtonText="Update input"
          values={input.attributes}
        />
      )}
    </IfPermitted>
  );
};

export default EditInputButton;
