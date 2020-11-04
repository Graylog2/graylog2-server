// @flow strict
import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import Role from 'logic/roles/Role';
import { EnterprisePluginNotFound } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  role: Role,
};

const TeamsSection = ({ role }: Props) => {
  const teamsPlugin = PluginStore.exports('teams');
  const RoleTeamsAssignment = teamsPlugin?.[0]?.RoleTeamsAssignment;

  return (
    <SectionComponent title="Teams">
      {RoleTeamsAssignment ? <RoleTeamsAssignment role={role} /> : <EnterprisePluginNotFound featureName="teams" />}
    </SectionComponent>
  );
};

export default TeamsSection;
