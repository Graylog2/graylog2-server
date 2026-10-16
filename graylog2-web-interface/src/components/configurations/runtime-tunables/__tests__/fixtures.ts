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

import type { RuntimeTunableFeature, RuntimeTunableFeatureSummary, TunableSchema } from '../types';

export const passwordSchema: TunableSchema = {
  type: 'object',
  required: ['min_length', 'require_uppercase'],
  properties: {
    min_length: { type: 'integer', title: 'Minimum length', minimum: 1, maximum: 128, 'x-graylog-order': 1 },
    require_uppercase: { type: 'boolean', title: 'Require uppercase letters', 'x-graylog-order': 2 },
  },
};

export const rawListResponse = {
  features: [
    { id: 'password_policy', title: 'Password Policy', order: 800, tunable_count: 1 },
    { id: 'search', title: 'Search', order: 100, tunable_count: 0 },
  ],
};

export const rawFeatureResponse = {
  id: 'password_policy',
  title: 'Password Policy',
  order: 800,
  tunables: [
        {
          id: 'users.password_complexity',
          title: 'Password complexity',
          description: 'Requirements for local user passwords',
          schema: passwordSchema,
          value: { min_length: 8, require_uppercase: true },
          defaults: { min_length: 6, require_uppercase: false },
          is_default: false,
          can_edit: true,
        },
      ],
};

export const passwordFeatureSummary: RuntimeTunableFeatureSummary = {
  id: 'password_policy',
  title: 'Password Policy',
  tunableCount: 1,
};

export const passwordFeature: RuntimeTunableFeature = {
  id: 'password_policy',
  title: 'Password Policy',
  tunables: [
    {
      id: 'users.password_complexity',
      title: 'Password complexity',
      description: 'Requirements for local user passwords',
      schema: passwordSchema,
      value: { min_length: 8, require_uppercase: true },
      defaults: { min_length: 6, require_uppercase: false },
      isDefault: false,
      canEdit: true,
    },
  ],
};
