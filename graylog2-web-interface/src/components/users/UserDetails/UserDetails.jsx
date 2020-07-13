// @flow strict
import * as React from 'react';
import styled from 'styled-components';

import { Spinner } from 'components/common';
import { Col, Row } from 'components/graylog';
import User from 'logic/users/User';

import SectionComponent from '../SectionComponent';
import SettingsSection from './SettingsSection';
import ProfileSection from './ProfileSection';

const MainDetails = styled(Row)`
  margin-bottom: 0;
`;

type Props = {
  user: User,
};

const UserDetails = ({ user }: Props) => {
  if (!user) {
    return <Spinner />;
  }

  return (
    <>
      <MainDetails>
        <Col md={6}>
          <ProfileSection user={user} />
          <SettingsSection user={user} />
        </Col>
        <Col md={6}>
          <SectionComponent title="Teams">Children</SectionComponent>
          <SectionComponent title="Roles">Children</SectionComponent>
        </Col>
      </MainDetails>
      <SectionComponent title="Entity Shares">Children</SectionComponent>
    </>
  );
};

export default UserDetails;
