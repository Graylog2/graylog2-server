// @flow strict
import * as React from 'react';
import { Formik, Form, Field } from 'formik';

import { Button, Row, Col } from 'components/graylog';
import User from 'logic/users/User';
import TimeoutInput from 'components/users/TimeoutInput';
import { TimezoneSelect } from 'components/common';

import SectionComponent from '../SectionComponent';
import FormRow from '../form/FormRow';

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
    <Formik onSubmit={(data) => onSubmit({ timezone: data.timezone, session_timeout_ms: data.timeout })}
            initialValues={{ timezone, timeout: sessionTimeoutMs }}>
      {({ isSubmitting, isValid }) => (
        <Form>
          <FormRow label={<label htmlFor="timeout">Sessions Timeout</label>}>
            <Field name="timeout">
              {({ field: { name, value, onChange } }) => (
                <TimeoutInput value={value}
                              labelSize={3}
                              controlSize={9}
                              onChange={(newValue) => onChange({ target: { name, value: newValue } })} />
              )}
            </Field>
          </FormRow>
          <FormRow label={<label htmlFor="timezone">Time Zone</label>}>
            <Field name="timezone">
              {({ field: { name, value, onChange } }) => (
                <TimezoneSelect className="timezone-select"
                                value={value}
                                name="timezone"
                                onChange={(newValue) => onChange({ target: { name, value: newValue } })} />
              )}
            </Field>
          </FormRow>
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
