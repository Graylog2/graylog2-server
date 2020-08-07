// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import { Formik, Form } from 'formik';

import { Alert, Col, Row, Button } from 'components/graylog';
import { UsersActions } from 'stores/users/UsersStore';
import UserNotification from 'util/UserNotification';
import history from 'util/History';
import Routes from 'routing/Routes';

import TimezoneFormGroup from './TimezoneFormGroup';
import TimeoutFormGroup from './TimeoutFormGroup';
import FullNameFormGroup from './FullNameFormGroup';
import EmailFormGroup from './EmailFormGroup';
import PasswordFormGroup, { validatePasswords } from './PasswordFormGroup';
import UsernameFormGroup from './UsernameFormGroup';

import { Headline } from '../../common/Section/SectionComponent';

const _onSubmit = (formData, setSubmitError) => {
  const data = { ...formData, permissions: [] };
  delete data.password_repeat;

  setSubmitError(null);

  UsersActions.create(data).then(() => {
    UserNotification.success(`User ${formData.username} was created successfully.`, 'Success!');
    history.push(Routes.SYSTEM.USERS.OVERVIEW);
  }, (error) => setSubmitError(error));
};

const _validate = (values) => {
  let errors = {};
  const { password, password_repeat: passwordRepeat } = values;
  errors = validatePasswords(errors, password, passwordRepeat);

  return errors;
};

const UserCreate = () => {
  const [users, setUsers] = useState();
  const [submitError, setSubmitError] = useState();

  useEffect(() => {
    UsersActions.loadUsers().then(setUsers);
  }, []);

  return (
    <Row className="content">
      <Col lg={8}>
        <Formik onSubmit={(data) => _onSubmit(data, setSubmitError)}
                validate={_validate}
                initialValues={{}}>
          {({ isSubmitting, isValid }) => (
            <Form className="form form-horizontal">
              <div>
                <Headline>Profile</Headline>
                <UsernameFormGroup users={users} />
                <FullNameFormGroup />
                <EmailFormGroup />
              </div>
              <div>
                <Headline>Settings</Headline>
                <TimeoutFormGroup />
                <TimezoneFormGroup />
              </div>
              <div>
                <Headline>Password</Headline>
                <PasswordFormGroup />
              </div>
              {submitError && (
                <Row>
                  <Col xs={9} xsOffset={3}>
                    <Alert bsStyle="danger">
                      <b>Failed to create user</b><br />
                      {submitError?.additional?.res?.text}
                    </Alert>
                  </Col>
                </Row>
              )}
              <Row>
                <Col xs={9} xsOffset={3}>
                  <Button bsStyle="success"
                          disabled={isSubmitting || !isValid}
                          title="Create User"
                          type="submit">
                    Create User
                  </Button>
                </Col>
              </Row>
            </Form>
          )}
        </Formik>
      </Col>
    </Row>
  );
};

export default UserCreate;
