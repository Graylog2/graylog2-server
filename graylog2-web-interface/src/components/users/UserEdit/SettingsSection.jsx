// @flow strict
import * as React from 'react';
import { Formik, Form } from 'formik';

import { Button, Row, Col } from 'components/graylog';
import User from 'logic/users/User';

import SectionComponent from '../SectionComponent';
import FormikTimeZoneSelect from '../UserCreate/FormikTimeZoneSelect';
import FormikTimoutInput from '../UserCreate/FormikTimoutInput';

type Props = {
  user: User,
  onSubmit: ({timezone: $PropertyType<User, 'timezone'> }) => Promise<void>,
};

const SettingsSection = ({
  user: {
    timezone,
    sessionTimeoutMs,
  },
  onSubmit,
}: Props) => (
  <SectionComponent title="Settings">
    <Formik onSubmit={onSubmit}
            initialValues={{ timezone, session_timeout_ms: sessionTimeoutMs }}>
      {({ isSubmitting, isValid }) => (
        <Form className="form form-horizontal">
          <FormikTimoutInput />
          <FormikTimeZoneSelect />
          <Row>
            <Col xs={12}>
              <div className="pull-right">
                <Button bsStyle="success"
                        disabled={isSubmitting || !isValid}
                        title="Update Profile"
                        type="submit">
                  Update Settings
                </Button>
              </div>
            </Col>
          </Row>
        </Form>
      )}
    </Formik>
  </SectionComponent>
);

export default SettingsSection;
