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

import type { SystemRuntimeTunables } from '@graylog/server-api';

import type { RuntimeTunableFeature, RuntimeTunableFeatureSummary, TunableSchema, TunableValues } from './types';

type ListResponse = Awaited<ReturnType<typeof SystemRuntimeTunables.list>>;
type FeatureResponse = Awaited<ReturnType<typeof SystemRuntimeTunables.getFeature>>;

export const parseFeatureSummaries = (response: ListResponse): Array<RuntimeTunableFeatureSummary> =>
  response.features.map((feature) => ({
    id: feature.id,
    title: feature.title,
    tunableCount: feature.tunable_count,
  }));

export const parseFeature = (feature: FeatureResponse): RuntimeTunableFeature => ({
  id: feature.id,
  title: feature.title,
  tunables: feature.tunables.map((tunable) => ({
    id: tunable.id,
    title: tunable.title,
    description: tunable.description,
    schema: tunable.schema as TunableSchema,
    value: tunable.value as TunableValues,
    defaults: tunable.defaults as TunableValues,
    isDefault: tunable.is_default,
    canEdit: tunable.can_edit,
  })),
});
