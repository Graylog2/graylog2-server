// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import { Formik, Form } from 'formik';

import { useStore } from 'stores/connect';
import { Alert, Col, Row, Button } from 'components/graylog';
import { UsersStore, UsersActions } from 'stores/users/UsersStore';
import UserNotification from 'util/UserNotification';
import history from 'util/History';
import Routes from 'routing/Routes';

import FormikTimeZoneSelect from './FormikTimeZoneSelect';
import FormikTimoutInput from './FormikTimoutInput';
import FormikFullName from './FormikFullName';
import FormikEmail from './FormikEmail';
import FormikPassword, { validatePasswords } from './FormikPassword';
import FormikUsername from './FormikUsername';

import { Headline } from '../SectionComponent';

const _onSubmit = (formData, setSubmitError) => {
  const data = { ...formData, permissions: [] };
  delete data.password_repeat;

  setSubmitError(null);

  UsersActions.create(data).then(() => {
    UserNotification.success(`User ${formData.username} was created successfully.`, 'Success!');
    history.replace(Routes.SYSTEM.USERS.OVERVIEW);
  }, (error) => setSubmitError(error));
};

const _validate = (values) => {
  let errors = {};
  const { password, password_repeat: passwordRepeat } = values;
  errors = validatePasswords(errors, password, passwordRepeat);

  return errors;
};

const UserCreate = () => {
  const { list: users } = useStore(UsersStore);
  const [submitError, setSubmitError] = useState();

  useEffect(() => {
    UsersActions.loadUsers();
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
                <FormikUsername users={users} />
                <FormikFullName />
                <FormikEmail />
              </div>
              <div>
                <Headline>Settings</Headline>
                <FormikTimoutInput />
                <FormikTimeZoneSelect />
              </div>
              <div>
                <Headline>Password</Headline>
                <FormikPassword />
              </div>
              <Row>
                <Col xs={9} xsOffset={3}>
                  {submitError && (
                  <Alert bsStyle="danger">
                    <b>Failed to create user</b><br />
                    {submitError?.additional?.res?.text}
                  </Alert>
                  )}
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
