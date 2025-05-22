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
import { useState, useEffect } from 'react';

import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';
import { getConfig } from 'components/configurations/helpers';

const useTokenTTL = (
  fallbackDefaultTokenTTL: string,
): {
  tokenTtl: string;
  defaultTokenTtl: string;
  setTokenTtl: (ttl: string) => void;
  resetTokenTtl: () => void;
} => {
  const [tokenTtl, setTokenTtl] = useState(fallbackDefaultTokenTTL);
  const [defaultTokenTtl, setDefaultTokenTtl] = useState(fallbackDefaultTokenTTL);
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);

  useEffect(() => {
    ConfigurationsActions.listUserConfig(ConfigurationType.USER_CONFIG).then(() => {
      const config = getConfig(ConfigurationType.USER_CONFIG, configuration);

      if (config?.default_ttl_for_new_tokens) {
        setTokenTtl(config?.default_ttl_for_new_tokens);
        setDefaultTokenTtl(config?.default_ttl_for_new_tokens);
      }
    });
  }, [configuration]);

  const resetTokenTtl = () => {
    setTokenTtl(defaultTokenTtl);
  };

  return { tokenTtl, defaultTokenTtl, setTokenTtl, resetTokenTtl };
};

export default useTokenTTL;
