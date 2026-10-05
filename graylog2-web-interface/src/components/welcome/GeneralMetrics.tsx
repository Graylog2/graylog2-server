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

import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';
import usePluginEntities from 'hooks/usePluginEntities';

import GeneralWelcomeMetrics from './GeneralWelcomeMetrics';

const GeneralMetrics = () => {
  const {
    data: { valid: isValidSecurityLicense },
  } = usePluggableLicenseCheck('/license/security');
  const pluginContext = { isValidSecurityLicense };

  const generalPageMetricsPlugins = usePluginEntities('welcomePageMetrics.general');
  const activeGeneralPlugins = generalPageMetricsPlugins.filter((plugin) =>
    typeof plugin.isEnabled === 'function' ? plugin.isEnabled(pluginContext) : true,
  );

  const GeneralComponent = activeGeneralPlugins[0]?.component ?? GeneralWelcomeMetrics;

  return <GeneralComponent />;
};

export default GeneralMetrics;
