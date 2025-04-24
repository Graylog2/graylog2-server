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

import type { BrandingResource, BrandingResourceKey } from 'util/AppConfig';
import AppConfig from 'util/AppConfig';

const defaultResourcesFeeds: Record<BrandingResourceKey, string> = {
  stream_rule_matcher_code: '',
  contact_sales: '',
  contact_support: '',
  contact_us: '',
  enterprise_product: '',
};

const useResourcesCustomization = (brandingKey: BrandingResourceKey): BrandingResource => {
  const resources = useMemo(() => {
    const customResources = AppConfig?.branding?.()?.resources ?? {};

    return Object.fromEntries(
      Object.entries(customResources).map(([key, resource]: [BrandingResourceKey, BrandingResource]) => [
        key,
        { enabled: resource.enabled !== false, feed: resource.feed ?? defaultResourcesFeeds[key] },
      ]),
    );
  }, []);

  return resources[brandingKey];
};

export default useResourcesCustomization;
