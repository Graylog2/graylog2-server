// @flow strict
import * as React from 'react';

import { Row, Col } from 'components/graylog';
import { FormikInput } from 'components/common';
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
          <FormikInput name="password"
                       maxLength={100}
                       type="password"
                       placeholder="Password"
                       required
                       formGroupClassName="form-group no-bm"
                       wrapperClassName="col-xs-12"
                       minLength={6} />
        </Col>
        <Col sm={6}>
          <FormikInput name="password_repeat"
                       maxLength={100}
                       type="password"
                       placeholder="Repeat password"
                       formGroupClassName="form-group no-bm"
                       required
                       wrapperClassName="col-xs-12"
                       minLength={6} />
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
