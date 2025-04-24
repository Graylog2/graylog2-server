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
import Version from 'util/Version';

const defaultResourcesFeeds: Record<BrandingResourceKey, string> = {
  stream_rule_matcher_code: `https://github.com/Graylog2/graylog2-server/tree/${Version.getMajorAndMinorVersion()}/graylog2-server/src/main/java/org/graylog2/streams/matchers`,
  contact_sales: 'https://go2.graylog.org/contact-sales',
  contact_support: '',
  contact_us: 'https://www.graylog.org/community-support/',
  enterprise_product: 'https://www.graylog.org/graylog-enterprise-edition',
};

const useResourceCustomization = (brandingKey: BrandingResourceKey): BrandingResource => {
  const resources = useMemo(() => {
    const customResources = AppConfig?.branding?.()?.resources ?? {};

    return Object.fromEntries(
      Object.entries(defaultResourcesFeeds).map(([key, defaultFeed]: [BrandingResourceKey, string]) => {
        const resource = customResources?.[key];

        return [key, { enabled: resource?.enabled !== false, feed: resource?.feed ?? defaultFeed }];
      }),
    );
  }, []);

  return resources[brandingKey];
};

export default useResourceCustomization;
