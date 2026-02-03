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

import { Button } from 'components/bootstrap';
import { ConfigurationWell } from 'components/configurationforms';
import type { InputSummary } from 'hooks/usePaginatedInputs';
import InputStaticFields from 'components/inputs/InputStaticFields';
import type { InputTypeDescriptionsResponse } from 'hooks/useInputTypesDescriptions';
import SectionGrid from 'components/common/Section/SectionGrid';
import { ThroughputSection } from 'components/inputs/InputsOveriew';
import { InputForm } from 'components/inputs';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import type { ConfiguredInput } from 'components/messageloaders/Types';
import useInputMutations from 'hooks/useInputMutations';

type Props = {
  input: InputSummary;
  inputTypeDescriptions: InputTypeDescriptionsResponse;
};

const StyledSpan = styled.span`
  display: flex;
  justify-content: flex-end;
`;

const ExpandedConfigurationSection = ({ input, inputTypeDescriptions }: Props) => {
  const definition = inputTypeDescriptions[input.type] as any;
  const [showConfigurationForm, setShowConfigurationForm] = useState<boolean>(false);
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const { updateInput } = useInputMutations();

  const editInput = () => {
    setShowConfigurationForm(true);
  };
  const hanleInputUpdate = async (inputData: ConfiguredInput) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.INPUT_UPDATED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'input-edit',
    });

    await updateInput({ input: inputData, inputId: input.id });
  };

  return (
    <SectionGrid>
      <div>
        <SectionGrid>
          <span>
            {' '}
            {input.name} &nbsp; ({input.id})
          </span>
          <StyledSpan>
            <Button bsStyle="primary" bsSize="xsmall" onClick={editInput} disabled={definition === undefined}>
              Edit input
            </Button>
          </StyledSpan>
        </SectionGrid>
        <ConfigurationWell id={input.id} configuration={input.attributes} typeDefinition={definition} />
      </div>
      <div>
        <span>Throughput / Metrics</span>
        <br />
        <ThroughputSection input={input} />
        <InputStaticFields input={input} />
      </div>
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
    </SectionGrid>
  );
};

export default ExpandedConfigurationSection;
