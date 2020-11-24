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
import { SyntheticEvent } from 'react';

import { Input } from 'components/bootstrap';

import FormikInput from './FormikInput';

type Props = {
  autoComplete?: string,
  buttonAfter?: React.ReactElement | string,
  disabled?: boolean,
  label: React.ReactElement | string,
  name: string,
  onChange?: (event: SyntheticEvent<Input>) => void,
  labelClassName?: string,
  wrapperClassName?: string,
  formGroupClassName?: string,
  type?: string,
  error?: React.ReactElement | string,
  placeholder?: string
  help?: React.ReactElement | string,
  minLength?: number,
  maxLength?: number,
  required?: boolean,
  validate?: (any) => string | undefined,
};

/** Displays the FormikInput with a specific layout */
const FormikFormGroup = ({ labelClassName, wrapperClassName, label, name, onChange, ...rest }: Props) => (
  <FormikInput {...rest}
               label={label}
               id={name}
               onChange={onChange}
               name={name}
               labelClassName={labelClassName}
               wrapperClassName={wrapperClassName} />
);

FormikFormGroup.defaultProps = {
  autoComplete: undefined,
  buttonAfter: undefined,
  disabled: false,
  onChange: undefined,
  labelClassName: 'col-sm-3',
  wrapperClassName: 'col-sm-9',
  formGroupClassName: undefined,
  type: undefined,
  error: undefined,
  placeholder: undefined,
  help: undefined,
  minLength: undefined,
  maxLength: undefined,
  required: false,
  validate: () => undefined,
};

export default FormikFormGroup;
