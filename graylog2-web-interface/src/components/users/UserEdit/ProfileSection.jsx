// @flow strict
import * as React from 'react';
import { Formik, Form } from 'formik';

import StoreProvider from 'injection/StoreProvider';
import { Button, Col, Row } from 'components/graylog';
import { ReadOnlyFormGroup } from 'components/common';
import User from 'logic/users/User';
import SectionComponent from 'components/common/Section/SectionComponent';

import FullNameFormGroup from '../UserCreate/FullNameFormGroup';
import EmailFormGroup from '../UserCreate/EmailFormGroup';

const StartpageStore = StoreProvider.getStore('Startpage');

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

  const _resetStartpage = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`You are about to reset the startpage from user ${user.fullName}. Please confirm.`)) {
      StartpageStore.set(user.id);
    }
  };

  const isStartpageSet = !!user.startpage?.type;
  const resetTitle = isStartpageSet ? 'Reset startpage' : 'No startpage set';
  const resetButton = (
    <Button title={resetTitle}
            disabled={!isStartpageSet}
            onClick={_resetStartpage}>
      Reset Startpage
    </Button>
  );

  return (
    <SectionComponent title="Profile" headerActions={resetButton}>
      <Formik onSubmit={onSubmit}
              initialValues={{ email, full_name: fullName }}>
        {({ isSubmitting, isValid }) => (
          <Form className="form form-horizontal">
            <ReadOnlyFormGroup label="Username" value={username} />
            <FullNameFormGroup />
            <EmailFormGroup />
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
