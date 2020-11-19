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

import type { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';
import { ReadOnlyFormGroup } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';

import EditLinkButton from './EditLinkButton';

import { STEP_KEY as SERVER_CONFIG_KEY } from '../BackendWizard/ServerConfigStep';

type Props = {
  authenticationBackend: DirectoryServiceBackend,
};

const ServerConfigSection = ({ authenticationBackend }: Props) => {
  const { title, description, config: { servers = [], systemUserDn, systemUserPassword, transportSecurity, verifyCertificates } } = authenticationBackend;
  const serverUrls = servers.map((server) => `${server.host}:${server.port}`).join(', ');

  return (
    <SectionComponent title="Server Configuration" headerActions={<EditLinkButton authenticationBackendId={authenticationBackend.id} stepKey={SERVER_CONFIG_KEY} />}>
      <ReadOnlyFormGroup label="Title" value={title} />
      <ReadOnlyFormGroup label="Description" value={description} />
      <ReadOnlyFormGroup label="Server Address" value={serverUrls} />
      <ReadOnlyFormGroup label="System Username" value={systemUserDn} />
      <ReadOnlyFormGroup label="System Password" value={systemUserPassword?.isSet ? '******' : null} />
      <ReadOnlyFormGroup label="Transport Security" value={transportSecurity} />
      <ReadOnlyFormGroup label="Verify Certificates" value={verifyCertificates} />
    </SectionComponent>
  );
};

export default ServerConfigSection;
