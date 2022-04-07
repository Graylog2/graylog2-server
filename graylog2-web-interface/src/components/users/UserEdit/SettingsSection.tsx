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
import type { $PropertyType } from 'utility-types';

import { Button, Row, Col } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import type User from 'logic/users/User';
import SectionComponent from 'components/common/Section/SectionComponent';

import TimezoneFormGroup from '../UserCreate/TimezoneFormGroup';
import TimeoutFormGroup from '../UserCreate/TimeoutFormGroup';
import ServiceAccountFormGroup from '../UserCreate/ServiceAccountFormGroup';
import StartpageFormGroup from '../StartpageFormGroup';

type Props = {
  user: User,
  onSubmit: (payload: { timezone: $PropertyType<User, 'timezone'> }) => Promise<void>,
};

const SettingsSection = ({
  user: {
    id,
    timezone,
    sessionTimeoutMs,
    startpage,
    permissions,
    serviceAccount,
  },
  onSubmit,
}: Props) => (
  <SectionComponent title="Settings">
    <Formik onSubmit={onSubmit}
            initialValues={{ timezone, session_timeout_ms: sessionTimeoutMs, startpage, service_account: serviceAccount }}>
      {({ isSubmitting, isValid }) => (
        <Form className="form form-horizontal">
          <IfPermitted permissions="*">
            <TimeoutFormGroup />
          </IfPermitted>
          <TimezoneFormGroup />
          <IfPermitted permissions="user:edit">
            <ServiceAccountFormGroup />
          </IfPermitted>
          <StartpageFormGroup userId={id} permissions={permissions} />

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
