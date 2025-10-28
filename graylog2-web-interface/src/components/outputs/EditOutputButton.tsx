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

import { Button } from 'components/bootstrap';
import { ConfigurationForm } from 'components/configurationforms';

type EditOutputButtonProps = {
  output: any;
  disabled?: boolean;
  getTypeDefinition: (...args: any[]) => void;
  onUpdate?: (...args: any[]) => void;
};

const EditOutputButton = ({
  output,
  disabled = false,
  getTypeDefinition,
  onUpdate = () => {},
}: EditOutputButtonProps) => {
  const [typeDefinition, setTypeDefinition] = useState<any>(null);
  const onModalClose = () => setTypeDefinition(null);

  const handleClick = () => {
    getTypeDefinition(output.type, (definition: any) => {
      setTypeDefinition(definition.requested_configuration);
    });
  };

  const handleSubmit = (data: any) => {
    onUpdate(output, data);
    onModalClose();
  };

  return (
    <span>
      <Button disabled={disabled} onClick={handleClick}>
        Edit
      </Button>
      {typeDefinition && (
        <ConfigurationForm initialShow
                           cancelAction={() => onModalClose()}
                           key={`configuration-form-output-${output.id}`}
                           configFields={typeDefinition}
                           title={`Editing Output ${output.title}`}
                           typeName={output.type}
                           titleHelpText="Select a name of your new output that describes it."
                           submitAction={handleSubmit}
                           submitButtonText="Update output"
                           values={output.configuration}
                           titleValue={output.title} />
      )}
    </span>
  );
};

export default EditOutputButton;
