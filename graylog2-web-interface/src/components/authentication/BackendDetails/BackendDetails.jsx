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
import { useEffect, useState } from 'react';

import { getAuthServicePlugin } from 'util/AuthenticationService';
import type { PaginatedRoles } from 'actions/roles/AuthzRolesActions';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import { Spinner } from 'components/common';
import SectionGrid from 'components/common/Section/SectionGrid';

import SyncedUsersSection from './SyncedUsersSection';
import SyncedTeamsSection from './SyncedTeamsSection';

const _loadRoles = (setPaginatedRoles) => {
  const getUnlimited = { page: 1, perPage: 0, query: '' };

  AuthzRolesDomain.loadRolesPaginated(getUnlimited).then(setPaginatedRoles);
};

type Props = {
  authenticationBackend: AuthenticationBackend,
};

const BackendDetails = ({ authenticationBackend }: Props) => {
  const authService = getAuthServicePlugin(authenticationBackend.config.type);
  const [paginatedRoles, setPaginatedRoles] = useState<?PaginatedRoles>();

  useEffect(() => _loadRoles(setPaginatedRoles), []);

  if (!authService) {
    return `No authentication service plugin configured for type "${authenticationBackend.config.type}"`;
  }

  if (!paginatedRoles) {
    return <Spinner />;
  }

  const { configDetailsComponent: BackendConfigDetails } = authService;

  return (
    <SectionGrid>
      <div>
        <BackendConfigDetails authenticationBackend={authenticationBackend} roles={paginatedRoles.list} />
      </div>
      <div>
        <SyncedUsersSection authenticationBackend={authenticationBackend} roles={paginatedRoles.list} />
        <SyncedTeamsSection authenticationBackend={authenticationBackend} />
      </div>
    </SectionGrid>
  );
};

export default BackendDetails;
