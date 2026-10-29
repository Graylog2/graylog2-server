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

import './types';

import usePluginEntities from 'hooks/usePluginEntities';

export const UnlicensedText = () => {
  const { t } = useTranslation('system');

  return (
    <>
      <strong>{t('cluster_support_bundle_info.examine_content')}</strong>
      <br />
      {t('cluster_support_bundle_info.paid_license_only')}
    </>
  );
};

const ClusterSupportBundleInfo = () => {
  const { t } = useTranslation('system');
  const pluginLogger = usePluginEntities('supportBundle');
  const InfoComponent = pluginLogger[0]?.EnterpriseSupportBundleInfo || UnlicensedText;

  return (
    <p className="description">
      {t('cluster_support_bundle_info.description')}
      <br />
      {t('cluster_support_bundle_info.enterprise_customers')}
      <br />
      <br />
      {InfoComponent && <InfoComponent />}
    </p>
  );
};

export default ClusterSupportBundleInfo;
