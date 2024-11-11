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

import React from 'react';

import {
  BooleanField,
  DropdownField,
  EncryptedInlineBinaryField,
  ListField,
  NumberField,
  TextField,
} from 'components/configurationforms';
import type { ConfigurationField, FieldValue, EncryptedFieldValue } from 'components/configurationforms/types';

type Props = {
  typeName: string,
  configField: ConfigurationField,
  configKey: string,
  configValue?: FieldValue | EncryptedFieldValue<FieldValue>
  dirty?: boolean
  autoFocus?: boolean
  onChange: (field: string, value: FieldValue | EncryptedFieldValue<FieldValue>, dirty?: boolean) => void,
};

const ConfigurationFormField = ({ typeName, configField, configKey, configValue, dirty = false, autoFocus = false, onChange }: Props) => {
  const elementKey = `${typeName}-${configKey}`;

  switch (configField.type) {
    case 'text':
      return (
        <TextField key={elementKey}
                   typeName={typeName}
                   title={configKey}
                   field={configField}
                   value={configValue as string | EncryptedFieldValue<string>}
                   dirty={dirty}
                   onChange={onChange}
                   autoFocus={autoFocus} />
      );
    case 'number':
      return (
        <NumberField key={elementKey}
                     typeName={typeName}
                     title={configKey}
                     field={configField}
                     value={configValue as number}
                     onChange={onChange}
                     autoFocus={autoFocus} />
      );
    case 'boolean':
      return (
        <BooleanField key={elementKey}
                      typeName={typeName}
                      title={configKey}
                      field={configField}
                      value={configValue as boolean}
                      onChange={onChange}
                      autoFocus={autoFocus} />
      );
    case 'dropdown':
      return (
        <DropdownField key={elementKey}
                       typeName={typeName}
                       title={configKey}
                       field={configField}
                       value={configValue as string}
                       onChange={onChange}
                       autoFocus={autoFocus}
                       addPlaceholder />
      );
    case 'list':
      return (
        <ListField key={elementKey}
                   typeName={typeName}
                   title={configKey}
                   field={configField}
                   value={configValue as Array<string> | string}
                   onChange={onChange}
                   autoFocus={autoFocus} />
      );
    case 'inline_binary':
      if (configField.is_encrypted) {
        return (
          <EncryptedInlineBinaryField key={elementKey}
                                      typeName={typeName}
                                      title={configKey}
                                      field={configField}
                                      value={configValue as EncryptedFieldValue<string>}
                                      dirty={dirty}
                                      onChange={onChange}
                                      autoFocus={autoFocus} />
        );
      }

      return null;

    default:
      return null;
  }
};

export default ConfigurationFormField;
