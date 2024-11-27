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
import React, { useState } from 'react';

import { Button, Input } from 'components/bootstrap';
import { hasAttribute, optionalMarker } from 'components/configurationforms/FieldHelpers';
import { getValueFromInput } from 'util/FormsUtils';

import type { TextField as TextFieldType, EncryptedFieldValue } from './types';

type Props = {
  autoFocus?: boolean
  field: TextFieldType,
  dirty?: boolean
  onChange: (title: string, value: string | EncryptedFieldValue<string>, dirty?: boolean) => void,
  title: string,
  typeName: string,
  value?: string | EncryptedFieldValue<string>,
};

const TextField = ({ field, title, typeName, dirty = false, onChange, value = '', autoFocus = false }: Props) => {
  const isRequired = !field.is_optional;
  const showReadOnlyEncrypted = field.is_encrypted && !dirty && typeof value !== 'string' && value.is_set;
  const fieldType = (!hasAttribute(field.attributes, 'textarea') && (hasAttribute(field.attributes, 'is_password') || showReadOnlyEncrypted) ? 'password' : 'text');
  const fieldId = `${typeName}-${title}`;
  const [isResetted, setIsResetted] = useState<boolean>(false);

  const labelContent = <>{field.human_name} {optionalMarker(field)}</>;

  const getFieldValue = () => {
    if (showReadOnlyEncrypted) return 'encrypted placeholder';

    if (typeof value === 'string') return value;

    if (value && value.set_value) {
      return value.set_value;
    }

    return '';
  };

  const handleChange = ({ target }) => {
    const inputValue = getValueFromInput(target);

    if (field.is_encrypted) {
      onChange(title, { set_value: inputValue });
    } else {
      onChange(title, inputValue);
    }
  };

  const handleReset = () => {
    setIsResetted(true);
    onChange(title, { delete_value: true });
  };

  const handleUndoReset = () => {
    setIsResetted(false);
    onChange(title, { is_set: true }, false);
  };

  const buttonAfter = () => {
    if (isResetted) {
      return (
        <Button type="button" onClick={handleUndoReset}>
          Undo Reset
        </Button>
      );
    }

    if (!showReadOnlyEncrypted) {
      return null;
    }

    return (
      <Button type="button" onClick={handleReset}>
        Reset
      </Button>
    );
  };

  if (hasAttribute(field.attributes, 'textarea')) {
    return (
      <Input id={fieldId}
             type="textarea"
             rows={10}
             label={labelContent}
             name={`configuration[${title}]`}
             required={isRequired}
             help={field.description}
             value={getFieldValue()}
             onChange={handleChange}
             autoFocus={autoFocus} />
    );
  }

  return (
    <Input id={fieldId}
           type={fieldType}
           name={`configuration[${title}]`}
           label={labelContent}
           required={isRequired}
           help={field.description}
           value={getFieldValue()}
           readOnly={showReadOnlyEncrypted}
           onChange={handleChange}
           buttonAfter={buttonAfter()}
           autoFocus={autoFocus} />
  );
};

export default TextField;
