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

import { EnterprisePluginNotFound } from 'components/common';
import { getEnterpriseAuthenticationPlugin } from 'util/AuthenticationService';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  authenticationBackend: AuthenticationBackend,
};

const SyncedTeamsSection = ({ authenticationBackend }: Props) => {
  const enterpriseAuthenticationPlugin = getEnterpriseAuthenticationPlugin();
  const EnterpriseSyncedTeamsSection = enterpriseAuthenticationPlugin?.components.SyncedTeamsSection;

  if (!EnterpriseSyncedTeamsSection) {
    return (
      <SectionComponent title="Synchronized Teams">
        <EnterprisePluginNotFound featureName="teams" />
      </SectionComponent>
    );
  }

  return <EnterpriseSyncedTeamsSection authenticationBackend={authenticationBackend} />;
};

export default SyncedTeamsSection;
