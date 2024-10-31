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
import styled from 'styled-components';
import type { $PropertyType } from 'utility-types';

import Routes from 'routing/Routes';
import { Button, Row, Col } from 'components/bootstrap';
import { IfPermitted, NoSearchResult, ReadOnlyFormGroup } from 'components/common';
import type User from 'logic/users/User';
import type { StartPage } from 'logic/users/User';
import SectionComponent from 'components/common/Section/SectionComponent';

import TimezoneFormGroup from '../UserCreate/TimezoneFormGroup';
import TimeoutFormGroup from '../UserCreate/TimeoutFormGroup';
import ServiceAccountFormGroup from '../UserCreate/ServiceAccountFormGroup';
import StartpageFormGroup from '../StartpageFormGroup';
import useIsGlobalTimeoutEnabled from '../../../hooks/useIsGlobalTimeoutEnabled';
import { Link } from '../../common/router';

export type SettingsFormValues = {
  timezone: string,
  session_timeout_ms: number,
  startpage: StartPage | null | undefined,
  service_account: boolean,
}

const GlobalTimeoutMessage = styled(ReadOnlyFormGroup)`
  margin-bottom: 20px;
  
  .read-only-value-col {
    padding-top: 0;
  }
`;

type Props = {
  user: User,
  onSubmit: (payload: { timezone: $PropertyType<User, 'timezone'> }) => Promise<void>,
};

const _validate = async (values) => {
  let errors = {};

  const { type, id } = values.startpage ?? {};

  if (type && !id) {
    errors = { startpage: 'Please select an entity.' };
  }

  return errors;
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
}: Props) => {
  const isGlobalTimeoutEnabled = useIsGlobalTimeoutEnabled();

  return (
    <SectionComponent title="Settings">
      <Formik<SettingsFormValues> onSubmit={onSubmit}
                                  validate={_validate}
                                  initialValues={{ timezone, session_timeout_ms: sessionTimeoutMs, startpage, service_account: serviceAccount }}>
        {({ isSubmitting, isValid }) => (
          <Form className="form form-horizontal">
            <IfPermitted permissions="*">
              {isGlobalTimeoutEnabled ? (
                <GlobalTimeoutMessage label="Sessions Timeout"
                                      value={<NoSearchResult>User session timeout is not editable because the <IfPermitted permissions={['clusterconfigentry:read']}><Link to={Routes.SYSTEM.CONFIGURATIONS}>global session timeout</Link></IfPermitted> is enabled.</NoSearchResult>} />
              ) : (
                <TimeoutFormGroup />
              )}
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
};

export default SettingsSection;
