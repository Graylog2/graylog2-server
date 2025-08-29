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
import { useCallback, useMemo, useRef, useState } from 'react';

import { Button } from 'components/bootstrap';
import { ConfigurationForm, type ConfigurationField } from 'components/configurationforms';
import type { RefType } from 'components/configurationforms/ConfigurationForm';
import type { AvailableOutputSummary } from 'components/streams/useAvailableOutputTypes';

const formatOutputType = (type: AvailableOutputSummary, typeName: string) => (
  <option key={typeName} value={typeName}>
    {type.name}
  </option>
);

type CreateOutputDropdownProps = {
  getTypeDefinition: (...args: any[]) => void;
  types: { [key: string]: AvailableOutputSummary };
  onSubmit: (...args: any[]) => void;
};
const PLACEHOLDER = 'placeholder';

const CreateOutputDropdown = ({ types, getTypeDefinition, onSubmit }: CreateOutputDropdownProps) => {
  const configurationForm = useRef<RefType<{}>>();
  const [typeDefinition, setTypeDefinition] = useState<{
    [key: string]: ConfigurationField;
  }>({});
  const [typeName, setTypeName] = useState(PLACEHOLDER);

  const _openModal = useCallback(() => {
    if (typeName !== PLACEHOLDER && typeName !== '' && configurationForm.current) {
      configurationForm.current.open();
    }
  }, [typeName]);

  const _onTypeChange = useCallback(
    (evt: React.ChangeEvent<HTMLSelectElement>) => {
      const outputType = evt.target.value;

      setTypeName(evt.target.value);

      getTypeDefinition(outputType, (definition) => {
        setTypeDefinition(definition.requested_configuration);
      });
    },
    [getTypeDefinition],
  );
  const outputTypes = useMemo(() => Object.entries(types).map(([name, type]) => formatOutputType(type, name)), [types]);

  return (
    <div>
      <div className="form-inline">
        <select
          id="input-type"
          defaultValue={PLACEHOLDER}
          value={typeName}
          onChange={_onTypeChange}
          className="form-control">
          <option value={PLACEHOLDER} disabled>
            Select Output Type
          </option>
          {outputTypes}
        </select>
        &nbsp;
        <Button bsStyle="success" disabled={typeName === PLACEHOLDER} onClick={_openModal}>
          Launch new output
        </Button>
      </div>

      <ConfigurationForm
        ref={configurationForm}
        key="configuration-form-output"
        configFields={typeDefinition}
        title="Create new Output"
        titleHelpText="Select a name of your new output that describes it."
        typeName={typeName}
        submitButtonText="Create output"
        submitAction={onSubmit}
      />
    </div>
  );
};

export default CreateOutputDropdown;
