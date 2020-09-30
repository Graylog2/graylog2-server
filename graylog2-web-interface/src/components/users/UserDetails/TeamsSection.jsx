// @flow strict
import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { IfEnterpriseLicense, MissingEnterprisePlugin } from 'components/common';
import User from 'logic/users/User';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  user: User,
};

const TeamsSection = ({ user }: Props) => {
  const teamsPlugin = PluginStore.exports('teams');
  const TeamEditComponent = teamsPlugin?.[0]?.TeamEditComponent;

  return (
    <SectionComponent title="Teams">
      <IfEnterpriseLicense renderChildrenOnInvalid={!!TeamEditComponent}>
        {TeamEditComponent
          ? <TeamEditComponent user={user} readOnly />
          : <MissingEnterprisePlugin pluginType="teams" />}
      </IfEnterpriseLicense>
    </SectionComponent>
  );
};

export default TeamsSection;
