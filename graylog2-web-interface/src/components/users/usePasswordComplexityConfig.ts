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
import { useEffect, useMemo } from 'react';

import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import {
  ConfigurationsActions,
  ConfigurationsStore,
  type PasswordComplexityConfigType,
} from 'stores/configurations/ConfigurationsStore';
import { getConfig } from 'components/configurations/helpers';
import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';
import { DEFAULT_PASSWORD_COMPLEXITY_CONFIG } from 'logic/users/passwordComplexity';

const usePasswordComplexityConfig = (): PasswordComplexityConfigType => {
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);

  useEffect(() => {
    ConfigurationsActions.listPasswordComplexityConfig(ConfigurationType.PASSWORD_COMPLEXITY_CONFIG);
  }, []);

  return useMemo(
    () => getConfig(ConfigurationType.PASSWORD_COMPLEXITY_CONFIG, configuration) ?? DEFAULT_PASSWORD_COMPLEXITY_CONFIG,
    [configuration],
  );
};

export default usePasswordComplexityConfig;
