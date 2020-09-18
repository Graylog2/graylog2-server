// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Field } from 'formik';

import { Input } from 'components/bootstrap';

type Props = {
  component: typeof Field,
  label?: string,
  name: string,
  type?: string,
  help?: React.Node,
  labelClassName?: string,
  wrapperClassName?: string,
  validate?: (string) => ?string,
};

const checkboxProps = (value) => {
  return { defaultChecked: value ?? false };
};

const inputProps = (value) => {
  return { value: value ?? '' };
};

/** Wraps the common Input component with a formik Field */
const FormikInput = ({ component: Component, name, type, help, validate, ...rest }: Props) => (
  <Component name={name} validate={validate}>
    {({ field: { value, onChange, onBlur }, meta: { error, touched } }) => {
      const typeSepcificProps = type === 'checkbox' || type === 'radio' ? checkboxProps(value) : inputProps(value);

      return (
        <Input {...rest}
               {...typeSepcificProps}
               onBlur={onBlur}
               bsStyle={error ? 'error' : undefined}
               help={error ?? help}
               id={name}
               onChange={onChange}
               type={type} />
      );
    }}
  </Component>
);

FormikInput.propTypes = {
  component: PropTypes.func,
  help: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
  labelClassName: PropTypes.string,
  type: PropTypes.string,
  label: PropTypes.string,
  name: PropTypes.string.isRequired,
  wrapperClassName: PropTypes.string,
  validate: PropTypes.func,
};

FormikInput.defaultProps = {
  component: Field,
  help: undefined,
  labelClassName: undefined,
  type: 'text',
  label: undefined,
  validate: () => {},
  wrapperClassName: undefined,
};

export default FormikInput;
