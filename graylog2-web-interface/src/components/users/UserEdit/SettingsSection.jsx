// @flow strict
import * as React from 'react';
import { Formik, Form } from 'formik';

import { Button, Row, Col } from 'components/graylog';
import User from 'logic/users/User';
import SectionComponent from 'components/common/Section/SectionComponent';

import TimezoneFormGroup from '../UserCreate/TimezoneFormGroup';
import TimeoutFormGroup from '../UserCreate/TimeoutFormGroup';

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
          <TimeoutFormGroup />
          <TimezoneFormGroup />
          <Row className="no-bm">
            <Col xs={12}>
              <div className="pull-right">
                <Button bsStyle="success"
                        disabled={isSubmitting || !isValid}
                        title="Update Settings"
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
