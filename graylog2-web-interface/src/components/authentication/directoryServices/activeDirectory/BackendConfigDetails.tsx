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
import * as Immutable from 'immutable';

import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
import { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';
import Role from 'logic/roles/Role';

import ServerConfigSection from '../BackendConfigDetails/ServerConfigSection';
import UserSyncSection from '../BackendConfigDetails/UserSyncSection';
import GroupSyncSection from '../BackendConfigDetails/GroupSyncSection';

type Props = {
  authenticationBackend: DirectoryServiceBackend,
  roles: Immutable.List<Role>,
};

const BackendConfigDetails = ({ authenticationBackend, roles }: Props) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const {
    excludedFields: groupSyncExcludedFields = {},
  } = enterpriseGroupSyncPlugin?.wizardConfig?.activeDirectory ?? {};
  const excludedFields = { ...groupSyncExcludedFields, userUniqueIdAttribute: true };

  return (
    <>
      <ServerConfigSection authenticationBackend={authenticationBackend} />
      <UserSyncSection authenticationBackend={authenticationBackend} roles={roles} excludedFields={excludedFields} />
      <GroupSyncSection authenticationBackend={authenticationBackend} roles={roles} excludedFields={excludedFields} />
    </>
  );
};

export default BackendConfigDetails;
