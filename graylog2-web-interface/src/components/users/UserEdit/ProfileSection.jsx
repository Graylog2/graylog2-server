// @flow strict
import * as React from 'react';
import { Formik, Form } from 'formik';

import { Button, Col, Row } from 'components/graylog';
import User from 'logic/users/User';

import SectionComponent from '../SectionComponent';
import FormField from '../form/FormField';
import FormFieldRead from '../form/FormFieldRead';

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
      <Formik onSubmit={(data) => onSubmit({ email: data.email, full_name: data.fullName })}
              initialValues={{ email, fullName }}>
        {({ isSubmitting, isValid }) => (
          <Form className="form form-horizontal">
            <FormFieldRead label="Username" value={username} />
            <FormField label="Full Name" name="fullName" required />
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
