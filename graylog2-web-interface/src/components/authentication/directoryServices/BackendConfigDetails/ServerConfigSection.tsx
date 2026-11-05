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
import { useTranslation } from 'react-i18next';

import type { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';
import { ReadOnlyFormGroup } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';

import EditLinkButton from './EditLinkButton';

import { STEP_KEY as SERVER_CONFIG_KEY } from '../BackendWizard/ServerConfigStep';

type Props = {
  authenticationBackend: DirectoryServiceBackend;
};

const ServerConfigSection = ({ authenticationBackend }: Props) => {
  const { t } = useTranslation('authentication');
  const {
    title,
    description,
    config: { servers = [], systemUserDn, systemUserPassword, transportSecurity, verifyCertificates },
  } = authenticationBackend;
  const serverUrls = servers.map((server) => `${server.host}:${server.port}`).join(', ');

  return (
    <SectionComponent
      title={t('server_config.title')}
      headerActions={<EditLinkButton authenticationBackendId={authenticationBackend.id} stepKey={SERVER_CONFIG_KEY} />}>
      <ReadOnlyFormGroup label={t('server_config.title_label')} value={title} />
      <ReadOnlyFormGroup label={t('server_config.description_label')} value={description} />
      <ReadOnlyFormGroup label={t('server_config.server_address')} value={serverUrls} />
      <ReadOnlyFormGroup label={t('server_config.system_username')} value={systemUserDn} />
      <ReadOnlyFormGroup
        label={t('server_config.system_password')}
        value={systemUserPassword?.isSet ? '******' : null}
      />
      <ReadOnlyFormGroup label={t('server_config.transport_security')} value={transportSecurity} />
      <ReadOnlyFormGroup label={t('server_config.verify_certificates')} value={verifyCertificates} />
    </SectionComponent>
  );
};

export default ServerConfigSection;
