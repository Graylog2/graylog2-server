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
import { EnterprisePluginNotFound } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';

import EditLinkButton from './EditLinkButton';

import { STEP_KEY as GROUP_SYNC_KEY } from '../BackendWizard/GroupSyncStep';

type Props = {
  authenticationBackend: DirectoryServiceBackend,
  excludedFields?: {[ inputName: string ]: boolean },
  roles: Immutable.List<Role>,
};

const GroupSyncSection = ({ authenticationBackend, roles, excludedFields }: Props) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const GroupSyncSectionPlugin = enterpriseGroupSyncPlugin?.components.GroupSyncSection;

  if (!GroupSyncSectionPlugin) {
    return (
      <SectionComponent title="Group Synchronization"
                        headerActions={(
                          <EditLinkButton authenticationBackendId={authenticationBackend.id}
                                          stepKey={GROUP_SYNC_KEY} />
                      )}>
        <EnterprisePluginNotFound featureName="group synchronization" />
      </SectionComponent>
    );
  }

  return (
    <GroupSyncSectionPlugin authenticationBackend={authenticationBackend}
                            excludedFields={excludedFields}
                            roles={roles} />
  );
};

GroupSyncSection.defaultProps = {
  excludedFields: undefined,
};

export default GroupSyncSection;
