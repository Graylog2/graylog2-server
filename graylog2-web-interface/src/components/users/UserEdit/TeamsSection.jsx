// @flow strict
import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import User from 'logic/users/User';
import { EnterprisePluginNotFound } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  user: User,
};

const TeamsSection = ({ user }: Props) => {
  const teamsPlugin = PluginStore.exports('teams');
  const TeamEditComponent = teamsPlugin?.[0]?.TeamEditComponent;

  return (
    <SectionComponent title="Teams">
      {TeamEditComponent ? <TeamEditComponent user={user} /> : <EnterprisePluginNotFound featureName="teams" />}
    </SectionComponent>
  );
};

export default TeamsSection;
