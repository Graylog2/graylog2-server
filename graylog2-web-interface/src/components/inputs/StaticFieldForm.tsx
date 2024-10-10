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

import { BootstrapModalForm, Input } from 'components/bootstrap';
import type { Input as InputType } from 'components/messageloaders/Types';
import { InputStaticFieldsStore } from 'stores/inputs/InputStaticFieldsStore';

type Props = {
  input: InputType;
  setShowModal: (boolean) => void;
}

const StaticFieldForm = ({ input, setShowModal } : Props) => {
  const [fieldName, setFieldName] = useState<string>('');
  const [fieldValue, setFieldValue] = useState<string>('');

  const addStaticField = () => {
    InputStaticFieldsStore.create(input, fieldName, fieldValue).then(() => setShowModal(false));
  };

  const handleFieldChange = (name: string, event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.target;

    if (name === 'name') {
      setFieldName(value);
    }

    if (name === 'value') {
      setFieldValue(value);
    }
  };

  return (
    <BootstrapModalForm show
                        title="Add static field"
                        submitButtonText="Add field"
                        onCancel={() => { setShowModal(false); }}
                        onSubmitForm={addStaticField}>
      <p>Define a static field that is added to every message that comes in via this input. The field is not
        overwritten If the message already has that key. Key must only contain alphanumeric characters or
        underscores and not be a reserved field.
      </p>
      <Input type="text"
             value={fieldName}
             onChange={(event) => { handleFieldChange('name', event); }}
             id="field-name"
             label="Field name"
             required
             pattern="[A-Za-z0-9_]*"
             title="Should consist only of alphanumeric characters and underscores."
             autoFocus />
      <Input value={fieldValue}
             onChange={(event) => { handleFieldChange('value', event); }}
             type="text"
             id="field-value"
             label="Field value"
             required />
    </BootstrapModalForm>
  );
};

export default StaticFieldForm;
