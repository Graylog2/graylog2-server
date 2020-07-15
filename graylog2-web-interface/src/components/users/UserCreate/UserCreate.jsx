// @flow strict
import * as React from 'react';
import { Formik, Form } from 'formik';

import { Col, Row, Button } from 'components/graylog';
import history from 'util/History';
import UsersActions from 'actions/users/UsersActions';
import UserNotification from 'util/UserNotification';
import Routes from 'routing/Routes';

import FormikTimeZoneSelect from './FormikTimeZoneSelect';
import FormikTimoutInput from './FormikTimoutInput';
import FormikFullName from './FormikFullName';
import FormikEmail from './FormikEmail';
import FormikPassword from './FormikPassword';

import FormField from '../form/FormField';
import { Headline } from '../SectionComponent';

const onSubmit = (formData) => {
  UsersActions.create(formData).then(() => {
    UserNotification.success(`User ${formData.username} was created successfully.`, 'Success!');
    history.replace(Routes.SYSTEM.AUTHENTICATION.USERS.LIST);
  }, () => {
    UserNotification.error('Failed to create user!', 'Error!');
  });
};

const UserCreate = () => {
  return (
    <>
      <Row className="content">
        <Col lg={8}>
          <Formik onSubmit={onSubmit}
                  initialValues={{}}>
            {({ isSubmitting, isValid }) => (
              <Form className="form form-horizontal">
                <div>
                  <Headline>Profile</Headline>
                  <FormField label="Username"
                             name="username"
                             required
                             help="Select a unique user name used to log in with." />
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
                  <Col xs={12}>
                    <div className="pull-right">
                      <Button bsStyle="success"
                              disabled={isSubmitting || !isValid}
                              title="Create User"
                              type="submit">
                        Create User
                      </Button>
                    </div>
                  </Col>
                </Row>
              </Form>
            )}
          </Formik>
        </Col>
      </Row>
    </>
  );
};

export default UserCreate;
