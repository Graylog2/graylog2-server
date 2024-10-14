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

import type { SystemConfigurationComponentProps } from 'views/types';
import type { RetentionJsonSchema } from 'components/indices/Types';

type IndexRetentionConfigProp = object;

interface IndexRetentionConfigComponentProps extends SystemConfigurationComponentProps {
  config: IndexRetentionConfig;
  jsonSchema: RetentionJsonSchema;
  updateConfig: (update: IndexRetentionConfigProp) => void;
  useMaxNumberOfIndices: () => [
    number | undefined,
    React.Dispatch<React.SetStateAction<number>>
  ]
}

type IndexRetentionSummaryComponentProps = {
  config: IndexRetentionConfig;
};

type IndexRetentionConfig = {
  type: string;
  displayName: string;
  configComponent: React.ComponentType<IndexRetentionConfigComponentProps>;
  summaryComponent: React.ComponentType<IndexRetentionSummaryComponentProps>;
}

declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    indexRetentionConfig?: Array<IndexRetentionConfig>;
  }
}
