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
import { OrderedMap } from 'immutable';
import moment from 'moment';

import type { UrlQueryFilters } from 'components/common/EntityFilters/types';

import { useCollectorsConfig } from './useCollectorsConfig';

const useDefaultInstanceFilters = (): UrlQueryFilters | undefined => {
  const { data: config } = useCollectorsConfig();

  if (!config?.collector_default_visibility_threshold) {
    return undefined;
  }

  const threshold = moment.duration(config.collector_default_visibility_threshold).asSeconds();

  return OrderedMap({ last_seen: [`relative@${threshold}`] });
};

export default useDefaultInstanceFilters;
