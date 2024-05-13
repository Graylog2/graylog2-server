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

import type { IndexSetsDefaultConfiguration, IndexSetsDefaultConfigurationFormValues } from 'stores/indices/IndexSetsStore';
import type { RetentionStrategyConfig, RotationStrategyConfig } from 'components/indices/Types';

export type IndexSetTemplate = {
  id: string,
  title: string,
  description: string,
  built_in: boolean,
  index_set_config: IndexSetsDefaultConfiguration,
}

export type IndexSetTemplateFormValues = {
  id: string,
  title: string,
  description: string,
  built_in: boolean,
  rotation_strategy?: RotationStrategyConfig,
  rotation_strategy_class?: string,
  retention_strategy?: RetentionStrategyConfig,
  retention_strategy_class?: string,
  index_set_config: IndexSetsDefaultConfigurationFormValues,
}
