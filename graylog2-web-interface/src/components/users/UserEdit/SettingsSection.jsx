// @flow strict
import * as React from 'react';
import { Formik, Form, Field } from 'formik';

import { Button, Row, Col } from 'components/graylog';
import { Input } from 'components/bootstrap';
import User from 'logic/users/User';
import TimeoutInput from 'components/users/TimeoutInput';
import { TimezoneSelect } from 'components/common';

import SectionComponent from '../SectionComponent';

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
        <Form className="form form-horizontal">

          <Field name="timeout">
            {({ field: { name, value, onChange } }) => (
              <TimeoutInput value={value}
                            labelSize={3}
                            controlSize={9}
                            onChange={(newValue) => onChange({ target: { name, value: newValue } })} />
            )}
          </Field>

          <Field name="timezone">
            {({ field: { name, value, onChange } }) => (
              <Input id="timezone-select"
                     label="Time Zone"
                     help="Choose your local time zone or leave it as it is to use the system's default."
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9">
                <TimezoneSelect className="timezone-select"
                                value={value}
                                name="timezone"
                                onChange={(newValue) => onChange({ target: { name, value: newValue } })} />
              </Input>
            )}
          </Field>

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
