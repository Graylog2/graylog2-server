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
import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';

import type HTTPHeaderAuthConfig from 'logic/authentication/HTTPHeaderAuthConfig';
import HTTPHeaderAuthConfigDomain from 'domainActions/authentication/HTTPHeaderAuthConfigDomain';
import { Spinner, ReadOnlyFormGroup } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';

const HTTPHeaderAuthConfigSection = () => {
  const { t } = useTranslation('authentication');
  const [loadedConfig, setLoadedConfig] = useState<HTTPHeaderAuthConfig | undefined>();
  const sectionTitle = t('trusted_header.title');

  useEffect(() => {
    HTTPHeaderAuthConfigDomain.load().then(setLoadedConfig);
  }, []);

  if (!loadedConfig) {
    return (
      <SectionComponent title={sectionTitle}>
        <Spinner />
      </SectionComponent>
    );
  }

  return (
    <SectionComponent title={sectionTitle}>
      <p>{t('trusted_header.description')}</p>
      <ReadOnlyFormGroup label={t('trusted_header.enabled')} value={loadedConfig.enabled} />
      <ReadOnlyFormGroup label={t('trusted_header.username_header')} value={loadedConfig.usernameHeader} />
    </SectionComponent>
  );
};

export default HTTPHeaderAuthConfigSection;
