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
import { OTHER_BUCKET_NAME } from 'views/Constants';
import type { FieldValue } from 'views/logic/fieldtypes/FieldType';
import type { ActionContexts } from 'views/types';

// The synthetic "(Other)" bucket does not correspond to a real field value, so it cannot be
// turned into a meaningful query, filter or alert condition. Value actions that build one of
// those from the clicked value (or, for multi-value visualizations, from `contexts.valuePath`)
// must be disabled whenever that value is involved.
const containsOtherBucketValue = (
  value: FieldValue | undefined,
  contexts: Partial<ActionContexts> | null | undefined,
): boolean =>
  value === OTHER_BUCKET_NAME || (contexts?.valuePath ?? []).some((path) => Object.values(path).includes(OTHER_BUCKET_NAME));

export default containsOtherBucketValue;
