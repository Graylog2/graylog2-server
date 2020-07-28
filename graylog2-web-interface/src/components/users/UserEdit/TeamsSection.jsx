// @flow strict
import * as React from 'react';
import styled from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import User from 'logic/users/User';

import SectionComponent from '../SectionComponent';

type Props = {
  user: User,
};

const Header = styled.h4`
  margin-bottom: 5px;
`;

const NoEnterpriseComponent = () => (
  <SectionComponent title="Teams">
    <Header>No enterprise plugin found</Header>
    <p>To use the <b>Teams</b> functionality you need to install the Graylog <b>Enterprise</b> plugin.</p>
  </SectionComponent>
);

const TeamsSection = ({ user }: Props) => {
  const teamsPlugin = PluginStore.exports('teams');

  if (!teamsPlugin || teamsPlugin.length <= 0) {
    return <NoEnterpriseComponent />;
  }

  const { TeamEditComponent } = teamsPlugin[0];

  return (
    <SectionComponent title="Teams">
      <TeamEditComponent user={user} />
    </SectionComponent>
  );
};

export default TeamsSection;
