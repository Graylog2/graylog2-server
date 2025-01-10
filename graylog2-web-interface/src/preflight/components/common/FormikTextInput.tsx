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
import { Field } from 'formik';

import TextInput from 'preflight/components/common/TextInput';

type Props = {
  name: string,
  label: React.ReactNode,
  placeholder?: string,
  type?: string
  required?: boolean,
}

const FormikTextInput = ({ name, placeholder, label, type, required = false }: Props) => (
  <Field name={name}>
    {({ field: { value, onChange, onBlur }, meta: { error: validationError } }) => (
      <TextInput onBlur={onBlur}
                 required={required}
                 id={name}
                 label={label}
                 type={type}
                 placeholder={placeholder}
                 value={value}
                 error={validationError}
                 onChange={onChange} />
    )}
  </Field>
);

export default FormikTextInput;
