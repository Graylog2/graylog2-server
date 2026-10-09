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
import { ClusterDeflector, SystemDeflector, SystemIndexRanges } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

export const recalculateIndexRanges = (indexSetId: string) =>
  defaultOnError(
    SystemIndexRanges.rebuildIndexSet(indexSetId),
    'Could not create a job to start index ranges recalculation',
    'Error starting index ranges recalculation',
  );

export const cycleActiveWriteIndex = (indexSetId: string) =>
  defaultOnError(
    ClusterDeflector.cycleByindexSetId(indexSetId).then(() => SystemDeflector.deflector(indexSetId)),
    'Cycling deflector failed',
  );
