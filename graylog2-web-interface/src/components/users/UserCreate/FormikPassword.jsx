// @flow strict
import * as React from 'react';
import { Field } from 'formik';

import { Row, Col } from 'components/graylog';
import { Input } from 'components/bootstrap';

const FormikPassword = () => (
  <Input id="password-field"
         label="Password"
         help="Passwords must be at least 6 characters long. We recommend using a strong password."
         labelClassName="col-sm-3"
         wrapperClassName="col-sm-9">
    <Row>
      <Col sm={6}>
        <Field name="password">
          {({ field: { name, value, onChange } }) => (
            <input className="form-control"
                   id={name}
                   name={name}
                   onChange={onChange}
                   type="password"
                   placeholder="Password"
                   required
                   minLength={6}
                   value={value} />
          )}
        </Field>
      </Col>
      <Col sm={6}>
        <Field name="password-repeat">
          {({ field: { name, value, onChange } }) => (
            <input className="form-control"
                   id={name}
                   name={name}
                   onChange={onChange}
                   type="password"
                   placeholder="Repeat password"
                   required
                   minLength={6}
                   value={value} />
          )}
        </Field>
      </Col>
    </Row>
  </Input>
);

export default FormikPassword;
