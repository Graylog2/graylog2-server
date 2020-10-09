// @flow strict
import * as React from 'react';

import { Row, Col } from 'components/graylog';
import { FormikInput } from 'components/common';
import { Input } from 'components/bootstrap';

export const PASSWORD_MIN_LENGTH = 6;

export const validatePasswords = (errors: { [name: string]: string }, password: string, passwordRepeat: string) => {
  const newErrors = { ...errors };

  if (password && password.length < PASSWORD_MIN_LENGTH) {
    newErrors.password = `Password must be at least ${PASSWORD_MIN_LENGTH} characters long`;
  }

  if (password && passwordRepeat) {
    const passwordMatches = password === passwordRepeat;

    if (!passwordMatches) {
      newErrors.password_repeat = 'Passwords do not match';
    }
  }

  return newErrors;
};

type Props = {};

// eslint-disable-next-line no-empty-pattern
const PasswordFormGroup = ({}: Props) => (
  <Input id="password-field"
         label="Password"
         help={`Passwords must be at least ${PASSWORD_MIN_LENGTH} characters long. We recommend using a strong password.`}
         labelClassName="col-sm-3"
         wrapperClassName="col-sm-9">
    <Row className="no-bm">
      <Col sm={6}>
        <FormikInput name="password"
                     maxLength={100}
                     type="password"
                     placeholder="Password"
                     required
                     formGroupClassName="form-group no-bm"
                     wrapperClassName="col-xs-12"
                     minLength={PASSWORD_MIN_LENGTH} />
      </Col>
      <Col sm={6}>
        <FormikInput name="password_repeat"
                     maxLength={100}
                     type="password"
                     placeholder="Repeat password"
                     formGroupClassName="form-group no-bm"
                     required
                     wrapperClassName="col-xs-12"
                     minLength={PASSWORD_MIN_LENGTH} />
      </Col>
    </Row>
  </Input>
);

PasswordFormGroup.defaultProps = {
  passwordRef: undefined,
  passwordRepeatRef: undefined,
};

export default PasswordFormGroup;
