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

import PropTypes from 'prop-types';

import type { IndexSetConfig } from 'stores/indices/IndexSetsStore';
import { RotationStrategyConfigPropType, RetentionStrategyConfigPropType } from 'components/indices/Types';
import type { RetentionStrategyConfig, RotationStrategyConfig } from 'components/indices/Types';
import type { DataTieringConfig, DataTieringFormValues } from 'components/indices/data-tiering';
import { dataTieringPropType } from 'components/indices/data-tiering';

type IndexSetDefaultFields = Pick<IndexSetConfig,
  'index_prefix' |
  'index_analyzer' |
  'shards' |
  'replicas' |
  'index_optimization_max_num_segments' |
  'index_optimization_disabled' |
  'field_type_refresh_interval'
>

export type IndexSetsDefaultConfiguration = IndexSetDefaultFields & {
  rotation_strategy_class?: string | null,
  rotation_strategy?: RotationStrategyConfig | null,
  retention_strategy_class?: string | null,
  retention_strategy?: RetentionStrategyConfig | null,
  use_legacy_rotation: boolean,
  data_tiering: DataTieringConfig
}

type IndexSetsDefaultConfigurationFormValues = IndexSetDefaultFields & {
  data_tiering: DataTieringFormValues
}

export type IndexSetTemplate = {
  id: string,
  title: string,
  description?: string,
  built_in: boolean,
  default: boolean,
  enabled: boolean,
  disabled_reason: string,
  index_set_config: IndexSetsDefaultConfiguration,
}

export type IndexSetTemplateFormValues = {
  id: string,
  title: string,
  description: string,
  rotation_strategy?: RotationStrategyConfig,
  rotation_strategy_class?: string,
  retention_strategy?: RetentionStrategyConfig,
  retention_strategy_class?: string,
  index_set_config: IndexSetsDefaultConfigurationFormValues,
}

const indexSetDefaultConfigurationPropType = PropTypes.shape({
  index_prefix: PropTypes.string,
  index_analyzer: PropTypes.string.isRequired,
  shards: PropTypes.number.isRequired,
  replicas: PropTypes.number.isRequired,
  index_optimization_max_num_segments: PropTypes.number.isRequired,
  index_optimization_disabled: PropTypes.bool.isRequired,
  field_type_refresh_interval: PropTypes.number.isRequired,
  rotation_strategy_class: PropTypes.string,
  rotation_strategy: RotationStrategyConfigPropType,
  retention_strategy_class: PropTypes.string,
  retention_strategy: RetentionStrategyConfigPropType,
  use_legacy_rotation: PropTypes.bool.isRequired,
  data_tiering: dataTieringPropType,
});

export const indexSetTemplatePropType = PropTypes.shape({
  id: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
  description: PropTypes.string,
  built_in: PropTypes.bool.isRequired,
  default: PropTypes.bool.isRequired,
  enabled: PropTypes.bool.isRequired,
  disabled_reason: PropTypes.string.isRequired,
  index_set_config: indexSetDefaultConfigurationPropType,
});
