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
import { useContext } from 'react';
import { Formik, Form } from 'formik';

import UsersDomain from 'domainActions/users/UsersDomain';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { Button, Row, Col } from 'components/graylog';
import User from 'logic/users/User';
import { isPermitted } from 'util/PermissionsMixin';
import SectionComponent from 'components/common/Section/SectionComponent';
import { FormikFormGroup } from 'components/common';

import { validatePasswords } from '../UserCreate/PasswordFormGroup';

type Props = {
  user: User,
};

const _validate = (values) => {
  let errors = {};
  const { password, password_repeat: passwordRepeat } = values;
  errors = validatePasswords(errors, password, passwordRepeat);

  return errors;
};

const _onSubmit = (formData, userId) => {
  const data = { ...formData };
  delete data.password_repeat;

  return UsersDomain.changePassword(userId, data);
};

const PasswordSection = ({ user: { id } }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  let requiresOldPassword = true;

  if (isPermitted(currentUser?.permissions, 'users:passwordchange:*')) {
    // Ask for old password if user is editing their own account
    requiresOldPassword = id === currentUser?.id;
  }

  return (
    <SectionComponent title="Password">
      <Formik onSubmit={(formData) => _onSubmit(formData, id)}
              validate={_validate}
              initialValues={{}}>
        {({ isSubmitting, isValid }) => (
          <Form className="form form-horizontal">
            {requiresOldPassword && (
              <FormikFormGroup label="Old Password"
                               name="old_password"
                               type="password"
                               maxLength={100}
                               required
                               labelClassName="col-sm-3"
                               wrapperClassName="col-sm-9" />
            )}
            <FormikFormGroup label="New Password"
                             name="password"
                             type="password"
                             help="Passwords must be at least 6 characters long. We recommend using a strong password."
                             maxLength={100}
                             minLength={6}
                             labelClassName="col-sm-3"
                             wrapperClassName="col-sm-9"
                             required />
            <FormikFormGroup label="Repeat Password"
                             name="password_repeat"
                             type="password"
                             minLength={6}
                             maxLength={100}
                             required
                             labelClassName="col-sm-3"
                             wrapperClassName="col-sm-9" />
            <Row className="no-bm">
              <Col xs={12}>
                <div className="pull-right">
                  <Button bsStyle="success"
                          disabled={isSubmitting || !isValid}
                          title="Change Password"
                          type="submit">
                    Change Password
                  </Button>
                </div>
              </Col>
            </Row>
          </Form>
        )}
      </Formik>
    </SectionComponent>
  );
};

export default PasswordSection;
