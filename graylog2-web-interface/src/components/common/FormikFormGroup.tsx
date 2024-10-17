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

import FormikInput from './FormikInput';

type Props = React.PropsWithChildren<{
  autoComplete?: string,
  buttonAfter?: React.ReactElement | string,
  children?: React.ReactElement,
  disabled?: boolean,
  label: React.ReactElement | string,
  name: string,
  onChange?: (event: React.ChangeEvent<HTMLInputElement>) => void,
  labelClassName?: string,
  wrapperClassName?: string,
  formGroupClassName?: string,
  type?: string,
  error?: React.ReactElement | string,
  placeholder?: string
  help?: React.ReactElement | string,
  min?: number,
  max?: number,
  minLength?: number,
  maxLength?: number,
  required?: boolean,
  bsSize?: 'large' | 'small' | 'xsmall',
  validate?: (arg: any) => string | undefined,
  rows?: number,
  autoFocus?: boolean,
}>;

/** Displays the FormikInput with a specific layout */
const FormikFormGroup = ({ children, disabled = false, required = false, validate = () => undefined, autoFocus = false, labelClassName = 'col-sm-3', wrapperClassName = 'col-sm-9', label, name, onChange, ...rest }: Props) => (
  <FormikInput {...rest}
               disabled={disabled}
               required={required}
               validate={validate}
               autoFocus={autoFocus}
               label={label}
               id={name}
               onChange={onChange}
               name={name}
               labelClassName={labelClassName}
               wrapperClassName={wrapperClassName}>
    {children}
  </FormikInput>
);

export default FormikFormGroup;
