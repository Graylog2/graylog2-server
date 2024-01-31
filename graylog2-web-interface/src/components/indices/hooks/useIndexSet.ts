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

import { useEffect, useState } from 'react';
import moment from 'moment';

import type { IndexSet } from 'stores/indices/IndexSetsStore';
import type {
  RotationStrategyConfig,
  RetentionStrategyConfig,
} from 'components/indices/Types';

const useIndexSet = (initialIndexSet, loadingIndexDefaultsConfig, indexDefaultsConfig) => {
  const [indexSet, setIndexSet] = useState(initialIndexSet);

  useEffect(() => {
    if (loadingIndexDefaultsConfig || !indexDefaultsConfig) return;

    const defaultIndexSet: IndexSet = {
      title: '',
      description: '',
      index_prefix: indexDefaultsConfig.index_prefix,
      writable: true,
      can_be_default: true,
      shards: indexDefaultsConfig.shards,
      replicas: indexDefaultsConfig.replicas,
      rotation_strategy_class: indexDefaultsConfig.rotation_strategy_class,
      rotation_strategy: indexDefaultsConfig.rotation_strategy_config as RotationStrategyConfig,
      retention_strategy_class: indexDefaultsConfig.retention_strategy_class,
      retention_strategy: indexDefaultsConfig.retention_strategy_config as RetentionStrategyConfig,
      index_analyzer: indexDefaultsConfig.index_analyzer,
      index_optimization_max_num_segments: indexDefaultsConfig.index_optimization_max_num_segments,
      index_optimization_disabled: indexDefaultsConfig.index_optimization_disabled,
      field_type_refresh_interval: moment.duration(indexDefaultsConfig.field_type_refresh_interval, indexDefaultsConfig.field_type_refresh_interval_unit).asMilliseconds(),
    };

    setIndexSet({ ...defaultIndexSet, ...initialIndexSet });
  }, [loadingIndexDefaultsConfig, indexDefaultsConfig, initialIndexSet]);

  return [indexSet, setIndexSet];
};

export default useIndexSet;
