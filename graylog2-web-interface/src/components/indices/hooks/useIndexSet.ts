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

import useIndexSetTemplateDefaults from 'components/indices/IndexSetTemplates/hooks/useIndexSetTemplateDefaults';
import useSelectedIndexSetTemplate from 'components/indices/IndexSetTemplates/hooks/useSelectedTemplate';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import type { RotationStrategyConfig, RetentionStrategyConfig } from 'components/indices/Types';

const withoutNullValues = (values: object) => Object.fromEntries(Object.entries(values).filter(([_, v]) => v != null));

const useIndexSet = (initialIndexSet?: IndexSet): IndexSet | undefined => {
  const { loadingIndexSetTemplateDefaults, indexSetTemplateDefaults } = useIndexSetTemplateDefaults();
  const { selectedIndexSetTemplate } = useSelectedIndexSetTemplate();

  if (loadingIndexSetTemplateDefaults || !indexSetTemplateDefaults) {
    return initialIndexSet;
  }

  const defaultIndexSet: IndexSet = {
    title: '',
    description: '',
    index_prefix: '',
    writable: true,
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
    field_restrictions: indexSetTemplateDefaults.field_restrictions,
    index_set_template_id: null,
  };

  if (initialIndexSet) {
    return { ...defaultIndexSet, ...withoutNullValues(initialIndexSet) };
  }

  if (selectedIndexSetTemplate) {
    const templateConfig = selectedIndexSetTemplate.index_set_config;
    const templateConfigWithDefaultStrategies = templateConfig.use_legacy_rotation
      ? { ...templateConfig, data_tiering: indexSetTemplateDefaults.data_tiering }
      : {
          ...templateConfig,
          rotation_strategy_class: indexSetTemplateDefaults.rotation_strategy_class,
          rotation_strategy: indexSetTemplateDefaults.rotation_strategy as RotationStrategyConfig,
          retention_strategy_class: indexSetTemplateDefaults.retention_strategy_class,
          retention_strategy: indexSetTemplateDefaults.retention_strategy as RetentionStrategyConfig,
        };

    return {
      ...defaultIndexSet,
      ...withoutNullValues(templateConfigWithDefaultStrategies),
      index_set_template_id: selectedIndexSetTemplate.id,
    };
  }

  return {
    ...defaultIndexSet,
    field_limit: indexSetTemplateDefaults.field_limit,
    dynamic_mapping_disabled: indexSetTemplateDefaults.dynamic_mapping_disabled,
  };
};

export default useIndexSet;
