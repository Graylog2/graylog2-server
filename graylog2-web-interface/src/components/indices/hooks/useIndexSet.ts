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
import type { Dispatch, SetStateAction } from 'react';

import useIndexSetTemplateDefaults from 'components/indices/IndexSetTemplates/hooks/useIndexSetTemplateDefaults';
import useSelectedIndexSetTemplate from 'components/indices/IndexSetTemplates/hooks/useSelectedTemplate';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import type {
  RotationStrategyConfig,
  RetentionStrategyConfig,
} from 'components/indices/Types';

const useIndexSet = (initialIndexSet?: IndexSet) :[IndexSet, Dispatch<SetStateAction<IndexSet>>] => {
  const { loadingIndexSetTemplateDefaults, indexSetTemplateDefaults } = useIndexSetTemplateDefaults();
  const [indexSet, setIndexSet] = useState(initialIndexSet);
  const { selectedIndexSetTemplate } = useSelectedIndexSetTemplate();

  useEffect(() => {
    if (loadingIndexSetTemplateDefaults || !indexSetTemplateDefaults) return;

    const defaultIndexSet: IndexSet = {
      title: '',
      description: '',
      index_prefix: indexSetTemplateDefaults.index_prefix,
      writable: true,
      can_be_default: true,
      shards: indexSetTemplateDefaults.shards,
      data_tiering: indexSetTemplateDefaults.data_tiering,
      replicas: indexSetTemplateDefaults.replicas,
      rotation_strategy_class: indexSetTemplateDefaults.rotation_strategy_class,
      rotation_strategy: indexSetTemplateDefaults.rotation_strategy as RotationStrategyConfig,
      retention_strategy_class: indexSetTemplateDefaults.retention_strategy_class,
      retention_strategy: indexSetTemplateDefaults.retention_strategy as RetentionStrategyConfig,
      index_analyzer: indexSetTemplateDefaults.index_analyzer,
      index_optimization_max_num_segments: indexSetTemplateDefaults.index_optimization_max_num_segments,
      index_optimization_disabled: indexSetTemplateDefaults.index_optimization_disabled,
      field_type_refresh_interval: indexSetTemplateDefaults.field_type_refresh_interval,
    };

    if (initialIndexSet) {
      const initialIndexWithoutNullValues = Object.fromEntries(Object.entries(initialIndexSet).filter(([_, v]) => v != null));
      setIndexSet({ ...defaultIndexSet, ...initialIndexWithoutNullValues });
    } else if (selectedIndexSetTemplate) {
      const indexSetTemplateConfig = selectedIndexSetTemplate.index_set_config;

      if (indexSetTemplateConfig.use_legacy_rotation) {
        indexSetTemplateConfig.data_tiering = indexSetTemplateDefaults.data_tiering;
      } else {
        indexSetTemplateConfig.rotation_strategy_class = indexSetTemplateDefaults.rotation_strategy_class;
        indexSetTemplateConfig.rotation_strategy = indexSetTemplateDefaults.rotation_strategy as RotationStrategyConfig;
        indexSetTemplateConfig.retention_strategy_class = indexSetTemplateDefaults.retention_strategy_class;
        indexSetTemplateConfig.retention_strategy = indexSetTemplateDefaults.retention_strategy as RetentionStrategyConfig;
      }

      const indexSetTemplateConfigWithoutNullValues = Object.fromEntries(Object.entries(indexSetTemplateConfig).filter(([_, v]) => v != null));

      setIndexSet({ ...defaultIndexSet, ...indexSetTemplateConfigWithoutNullValues });
    } else {
      setIndexSet({ ...defaultIndexSet });
    }
  }, [loadingIndexSetTemplateDefaults, indexSetTemplateDefaults, initialIndexSet, selectedIndexSetTemplate]);

  return [indexSet, setIndexSet];
};

export default useIndexSet;
