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
// @flow strict
import * as React from 'react';

import { Input } from 'components/bootstrap';

import FormikInput from './FormikInput';

type Props = {
  label: string,
  name: string,
  onChange?: (SyntheticInputEvent<Input>) => void,
  labelClassName?: string,
  wrapperClassName?: string,
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
  onChange: undefined,
  labelClassName: 'col-sm-3',
  wrapperClassName: 'col-sm-9',
};

export default FormikFormGroup;
