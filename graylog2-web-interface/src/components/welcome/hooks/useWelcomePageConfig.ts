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
import { useEffect } from 'react';

import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { ConfigurationsStore, ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import type { WelcomePageConfigType } from 'components/common/types';
import { DEFAULT_WELCOME_PAGE_CONFIG } from 'logic/welcome/welcomePageConfig';
import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';

type WelcomePageConfig = { metricsEnabled: boolean };

const useWelcomePageConfig = (): WelcomePageConfig => {
  const configuration = useStore(
    ConfigurationsStore as Store<Record<string, any>>,
    (state) => state?.configuration[ConfigurationType.WELCOME_PAGE_CONFIG] as WelcomePageConfigType,
  );

  useEffect(() => {
    ConfigurationsActions.list(ConfigurationType.WELCOME_PAGE_CONFIG);
  }, []);

  const disableMetrics = configuration?.disable_metrics ?? DEFAULT_WELCOME_PAGE_CONFIG.disable_metrics;

  return { metricsEnabled: !disableMetrics };
};

export default useWelcomePageConfig;
