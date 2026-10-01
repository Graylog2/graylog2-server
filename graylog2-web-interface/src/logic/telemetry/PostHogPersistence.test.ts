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
import posthog from 'posthog-js';

describe('PostHog localStorage+cookie persistence', () => {
  it('shrinks an oversized legacy cookie and keeps the identity', () => {
    const legacyState = {
      distinct_id: 'legacy-id',
      $user_state: 'identified',
      $stored_group_properties: { cluster: { details: 'x'.repeat(6000) } },
    };
    document.cookie = `ph_key_posthog=${encodeURIComponent(JSON.stringify(legacyState))}; path=/`;
    const legacyCookieSize = document.cookie.length;

    posthog.init('key', {
      api_host: 'http://localhost',
      autocapture: false,
      capture_pageview: false,
      cross_subdomain_cookie: false,
      persistence: 'localStorage+cookie',
    });

    expect(posthog.get_distinct_id()).toBe('legacy-id');
    expect(document.cookie.length).toBeLessThan(legacyCookieSize / 4);
  });
});
