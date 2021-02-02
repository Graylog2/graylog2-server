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
// @flow strict
import * as React from 'react';
import { Formik, Form } from 'formik';

import { Button, Col, Row } from 'components/graylog';
import { ReadOnlyFormGroup } from 'components/common';
import User from 'logic/users/User';
import SectionComponent from 'components/common/Section/SectionComponent';
import AppConfig from 'util/AppConfig';

import FullNameFormGroup from '../UserCreate/FullNameFormGroup';
import EmailFormGroup from '../UserCreate/EmailFormGroup';

const isCloud = AppConfig.isCloud();
type Props = {
  user: User,
  onSubmit: ({
    full_name: $PropertyType<User, 'fullName'>,
    email: $PropertyType<User, 'email'>,
    username: $PropertyType<User, 'username'>,
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

  const _getUserNameGroup = () => {
    if (isCloud) {
      return <ReadOnlyFormGroup label="Email" value={email} />;
    }

    return (
      <>
        <ReadOnlyFormGroup label="Username" value={username} />
      </>
    );
  };

  const _getEmailGroup = () => {
    if (isCloud) {
      return null;
    }

    return (
      <>
        <EmailFormGroup />
      </>
    );
  };

  return (
    <SectionComponent title="Profile">
      <Formik onSubmit={onSubmit}
              initialValues={{ email, full_name: fullName }}>
        {({ isSubmitting, isValid }) => (
          <Form className="form form-horizontal">
            <FullNameFormGroup />
            {_getUserNameGroup()}
            {_getEmailGroup()}
            <Row className="no-bm">
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
