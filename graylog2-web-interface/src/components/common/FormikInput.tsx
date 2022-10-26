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
import type { SyntheticEvent } from 'react';
import PropTypes from 'prop-types';
import { Field, useFormikContext } from 'formik';

import { Input } from 'components/bootstrap';

type BaseProps = {
  autoComplete?: string,
  bsSize?: 'large' | 'small' | 'xsmall',
  buttonAfter?: React.ReactElement | string,
  disabled?: boolean,
  error?: React.ReactElement | string,
  formGroupClassName?: string,
  help?: React.ReactElement | string,
  id: string,
  label?: React.ReactElement | string,
  labelClassName?: string,
  maxLength?: number,
  minLength?: number,
  name: string,
  onChange?: (event: SyntheticEvent<Input>) => void,
  placeholder?: string,
  required?: boolean,
  type?: string,
  validate?: (arg: string) => string | undefined | null,
  wrapperClassName?: string,
  autoFocus?: boolean,
};

type TextareaProps = BaseProps & {
  type: 'textarea',
  rows?: number
};
type Props = BaseProps | TextareaProps;

const checkboxProps = (value) => {
  return { defaultChecked: value ?? false };
};

const inputProps = (value) => {
  return { value: value ?? '' };
};

/** Wraps the common Input component with a formik Field */
const FormikInput = ({ name, type, help, validate, onChange: propagateOnChange, error: errorProp, ...rest }: Props) => {
  const { validateOnChange } = useFormikContext();

  return (
    <Field name={name} validate={validate}>
      {({ field: { value, onChange, onBlur }, meta: { error: validationError, touched } }) => {
        const typeSpecificProps = type === 'checkbox' ? checkboxProps(value) : inputProps(value);
        const displayValidationError = validateOnChange ? !!(validationError && touched) : !!validationError;
        const error = displayValidationError ? validationError : errorProp;

        const _handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
          if (typeof propagateOnChange === 'function') {
            propagateOnChange(e);
          }

          onChange(e);
        };

        return (
          <Input {...rest}
                 {...typeSpecificProps}
                 onBlur={onBlur}
                 help={help}
                 id={name}
                 error={error}
                 onChange={_handleChange}
                 type={type} />
        );
      }}
    </Field>
  );
};

FormikInput.propTypes = {
  autoComplete: PropTypes.string,
  bsSize: PropTypes.string,
  buttonAfter: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
  disabled: PropTypes.bool,
  error: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
  formGroupClassName: PropTypes.string,
  help: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
  label: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
  labelClassName: PropTypes.string,
  maxLength: PropTypes.number,
  minLength: PropTypes.number,
  name: PropTypes.string.isRequired,
  onChange: PropTypes.func,
  placeholder: PropTypes.string,
  required: PropTypes.bool,
  type: PropTypes.string,
  validate: PropTypes.func,
  wrapperClassName: PropTypes.string,
  autoFocus: PropTypes.bool,
};

FormikInput.defaultProps = {
  autoComplete: undefined,
  bsSize: undefined,
  buttonAfter: undefined,
  disabled: false,
  error: undefined,
  formGroupClassName: undefined,
  help: undefined,
  label: undefined,
  labelClassName: undefined,
  maxLength: undefined,
  minLength: undefined,
  onChange: undefined,
  placeholder: undefined,
  required: false,
  type: 'text',
  validate: () => undefined,
  wrapperClassName: undefined,
  autoFocus: false,
};

export default FormikInput;
