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
import { useState, useRef } from 'react';

import type { ConfigurationFormData } from 'components/configurationforms';
import { ConfigurationForm } from 'components/configurationforms';
import type { Output } from 'stores/outputs/OutputsStore';
import { Button } from 'components/bootstrap';
import { Icon, IfPermitted } from 'components/common';
import type { AvailableOutputRequestedConfiguration } from 'components/streams/useAvailableOutputTypes';

type Props = {
  output: Output;
  disabled?: boolean;
  onUpdate: (output: Output, data: ConfigurationFormData<Output['configuration']>) => void;
  getTypeDefinition: (type: string) => undefined | AvailableOutputRequestedConfiguration;
};

const EditOutputButton = ({ output, disabled = false, onUpdate, getTypeDefinition }: Props) => {
  const [typeDefinition, setTypeDefinition] = useState<AvailableOutputRequestedConfiguration>(undefined);
  const configFormRef = useRef(null);

  const onClick = () => {
    setTypeDefinition(getTypeDefinition(output.type));

    if (configFormRef.current) {
      configFormRef.current?.open();
    }
  };

  const handleUpdate = (data: ConfigurationFormData<Output['configuration']>) => onUpdate(output, data);

  return (
    <IfPermitted permissions={`outputs:edit:${output.id}`}>
      <Button
        bsStyle="link"
        disabled={disabled}
        bsSize="xsmall"
        onClick={onClick}
        title="Edit Output">
        <Icon name="edit_square" />
      </Button>
      <ConfigurationForm<Output['configuration']>
        ref={configFormRef}
        key={`configuration-form-output-${output.id}`}
        configFields={typeDefinition}
        title={`Editing Output ${output.title}`}
        typeName={output.type}
        titleHelpText="Select a name of your new output that describes it."
        submitAction={handleUpdate}
        submitButtonText="Update output"
        values={output.configuration}
        titleValue={output.title}
      />
    </IfPermitted>
  );
};

export default EditOutputButton;
