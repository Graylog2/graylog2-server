// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Field } from 'formik';

import { Input } from 'components/bootstrap';

type Props = {
  component: typeof Field,
  label: string,
  name: string,
  type?: string,
  help?: string,
  labelClassName?: string,
  wrapperClassName?: string,
  validate?: (string) => ?string,
};

const checkboxProps = (value) => {
  return { checked: value ?? false };
};

const inputProps = (value) => {
  return { value: value ?? '' };
};

/** Wraps the common Input component with a formik Field */
const FormikInput = ({ component: Component, label, name, type, help, validate, ...rest }: Props) => (
  <Component name={name} validate={validate}>
    {({ field: { value, onChange }, meta: { error } }) => {
      const typeSepcificProps = type === 'checkbox' ? checkboxProps(value) : inputProps(value);

      return (
        <Input {...rest}
               {...typeSepcificProps}
               bsStyle={error ? 'error' : undefined}
               help={error ?? help}
               id={name}
               label={label}
               name={name}
               onChange={onChange}
               type={type} />
      );
    }}
  </Component>
);

FormikInput.propTypes = {
  component: PropTypes.element,
  help: PropTypes.object,
  labelClassName: PropTypes.string,
  type: PropTypes.string,
  label: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  wrapperClassName: PropTypes.string,
  validate: PropTypes.func,
};

FormikInput.defaultProps = {
  component: Field,
  help: undefined,
  labelClassName: undefined,
  type: 'text',
  validate: () => {},
  wrapperClassName: undefined,
};

export default FormikInput;
