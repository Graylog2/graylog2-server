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
import { useMemo } from 'react';

import type { BrandingResources, BrandingResource } from 'util/AppConfig';
import AppConfig from 'util/AppConfig';

const useResourcesCustomization = () =>
  useMemo<BrandingResources>(() => {
    const customResources = AppConfig?.branding?.()?.resources ?? {};

    return Object.fromEntries<BrandingResource>(
      Object.entries(customResources).map(([key, resource]) => [
        key,
        { enabled: resource.enabled !== false, feed: resource.feed },
      ]),
    );
  }, []);

export default useResourcesCustomization;
