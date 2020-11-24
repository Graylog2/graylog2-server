/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { Formik, Form } from 'formik';
import { $PropertyType } from 'utility-types';

import { Button, Row, Col } from 'components/graylog';
import User from 'logic/users/User';
import SectionComponent from 'components/common/Section/SectionComponent';

import TimezoneFormGroup from '../UserCreate/TimezoneFormGroup';
import TimeoutFormGroup from '../UserCreate/TimeoutFormGroup';
import StartpageFormGroup from '../StartpageFormGroup';

type Props = {
  user: User,
  onSubmit: (payload: { timezone: $PropertyType<User, 'timezone'> }) => Promise<void>,
};

const SettingsSection = ({
  user: {
    timezone,
    sessionTimeoutMs,
    startpage,
  },
  onSubmit,
}: Props) => (
  <SectionComponent title="Settings">
    <Formik onSubmit={onSubmit}
            initialValues={{ timezone, session_timeout_ms: sessionTimeoutMs, startpage }}>
      {({ isSubmitting, isValid }) => (
        <Form className="form form-horizontal">
          <TimeoutFormGroup />
          <TimezoneFormGroup />
          <StartpageFormGroup />

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
