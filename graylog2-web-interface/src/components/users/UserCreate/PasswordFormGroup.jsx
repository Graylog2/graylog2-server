// @flow strict
import * as React from 'react';
import { Field } from 'formik';

import { Row, Col, FormGroup } from 'components/graylog';
import { FormikFieldError } from 'components/common';
import { Input } from 'components/bootstrap';

export const validatePasswords = (errors: { [name: string]: string }, password: string, passwordRepeat: string) => {
  const newErrors = { ...errors };

  if (password && passwordRepeat) {
    const passwordMatches = password === passwordRepeat;

    if (!passwordMatches) {
      newErrors.password_repeat = 'Passwords do not match';
    }
  }

  return newErrors;
};

const PasswordFormGroup = () => {
  return (
    <Input id="password-field"
           label="Password"
           help="Passwords must be at least 6 characters long. We recommend using a strong password."
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9">
      <Row className="no-bm">
        <Col sm={6}>
          <Field name="password">
            {({ field: { name, value, onChange }, meta: { error } }) => (
              <FormGroup validationState={error ? 'error' : null} className="no-bm">
                <Col xs={12}>
                  <input className="form-control"
                         id={name}
                         name={name}
                         onChange={onChange}
                         maxLength={100}
                         type="password"
                         placeholder="Password"
                         required
                         minLength={6}
                         value={value ?? ''} />
                  {error && <FormikFieldError>{error}</FormikFieldError>}
                </Col>
              </FormGroup>
            )}
          </Field>
        </Col>
        <Col sm={6}>
          <Field name="password_repeat">
            {({ field: { name, value, onChange }, meta: { error } }) => (
              <FormGroup validationState={error ? 'error' : null} className="no-bm">
                <Col xs={12}>
                  <input className="form-control"
                         id={name}
                         name={name}
                         onChange={onChange}
                         maxLength={100}
                         type="password"
                         placeholder="Repeat password"
                         required
                         minLength={6}
                         value={value ?? ''} />
                  {error && <FormikFieldError>{error}</FormikFieldError>}
                </Col>
              </FormGroup>
            )}
          </Field>
        </Col>
      </Row>
    </Input>
  );
};

PasswordFormGroup.defaultProps = {
  passwordRef: undefined,
  passwordRepeatRef: undefined,
};

export default PasswordFormGroup;
