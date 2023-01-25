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

import type { UserConfigType } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationsStore, ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';

const USER_CONFIG = 'org.graylog2.users.UserConfiguration';

const useIsGlobalTimeoutEnabled = () => {
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration[USER_CONFIG] as UserConfigType);
  const isGlobalTimeoutEnabled = configuration?.enable_global_session_timeout || false;

  useEffect(() => {
    ConfigurationsActions.list(USER_CONFIG);

    return () => {};
  }, []);

  return isGlobalTimeoutEnabled;
};

export default useIsGlobalTimeoutEnabled;
