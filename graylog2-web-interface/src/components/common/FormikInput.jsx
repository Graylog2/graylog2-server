// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Field, useFormikContext } from 'formik';

import { Input } from 'components/bootstrap';

type Props = {
  name: string,
  type?: string,
  help?: React.Node,
  labelClassName?: string,
  onChange?: (SyntheticInputEvent<Input>) => void,
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
const FormikInput = ({ name, type, help, validate, onChange: propagateOnChange, ...rest }: Props) => {
  const { validateOnChange } = useFormikContext();

  return (
    <Field name={name} validate={validate}>
      {({ field: { value, onChange, onBlur }, meta: { error, touched } }) => {
        const typeSpecificProps = type === 'checkbox' ? checkboxProps(value) : inputProps(value);
        const displayError = validateOnChange ? !!(error && touched) : !!error;

        const _handleChange = (e) => {
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
                 error={displayError ? error : undefined}
                 onChange={_handleChange}
                 type={type} />
        );
      }}
    </Field>
  );
};

FormikInput.propTypes = {
  help: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
  labelClassName: PropTypes.string,
  type: PropTypes.string,
  name: PropTypes.string.isRequired,
  wrapperClassName: PropTypes.string,
  validate: PropTypes.func,
};

FormikInput.defaultProps = {
  help: undefined,
  labelClassName: undefined,
  onChange: undefined,
  type: 'text',
  validate: () => {},
  wrapperClassName: undefined,
};

export default FormikInput;
