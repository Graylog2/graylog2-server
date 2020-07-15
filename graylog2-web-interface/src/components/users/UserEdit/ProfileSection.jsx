// @flow strict
import * as React from 'react';
import { Formik, Form } from 'formik';

import { Button, Col, Row } from 'components/graylog';
import User from 'logic/users/User';

import SectionComponent from '../SectionComponent';
import FormField from '../form/FormField';
import ReadOnlyFormField from '../form/ReadOnlyFormField';

type Props = {
  user: User,
  onSubmit: ({
    full_name: $PropertyType<User, 'fullName'>,
    email: $PropertyType<User, 'email'>,
  }) => Promise<void>,
};

const ProfileSection = ({
  user,
  onSubmit,
}: Props) => {
  const {
    username,
    fullName,
    email,
  } = user;

  return (
    <SectionComponent title="Profile">
      <Formik onSubmit={onSubmit}
              initialValues={{ email, full_name: fullName }}>
        {({ isSubmitting, isValid }) => (
          <Form className="form form-horizontal">
            <ReadOnlyFormField label="Username" value={username} />
            <FormField label="Full Name" name="full_name" required />
            <FormField label="E-Mail Address" name="email" type="email" required />
            <Row>
              <Col xs={12}>
                <div className="pull-right">
                  <Button bsStyle="success"
                          disabled={isSubmitting || !isValid}
                          title="Update Profile"
                          type="submit">
                    Update Profile
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

export default ProfileSection;
