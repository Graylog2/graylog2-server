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

import FormikFormGroup from '../form/FormikFormGroup';
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

const _onSubmit = (formData, username) => {
  const data = { ...formData };
  delete data.password_repeat;

  return UsersDomain.changePassword(username, data);
};

const PasswordSection = ({ user: { username } }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  let requiresOldPassword = true;

  if (isPermitted(currentUser?.permissions, 'users:passwordchange:*')) {
    // Ask for old password if user is editing their own account
    requiresOldPassword = username === currentUser?.username;
  }

  return (
    <SectionComponent title="Password">
      <Formik onSubmit={(formData) => _onSubmit(formData, username)}
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
                             required
                             labelClassName="col-sm-3"
                             wrapperClassName="col-sm-9" />
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
