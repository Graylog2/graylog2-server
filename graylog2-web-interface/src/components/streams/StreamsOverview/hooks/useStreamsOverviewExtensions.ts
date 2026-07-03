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
import usePluginEntities from 'hooks/usePluginEntities';
import usePluggableEntityTableElements from 'hooks/usePluggableEntityTableElements';
import type { Stream } from 'logic/streams/types';
import type { Attribute } from 'stores/PaginationTypes';
import type { ColumnRenderersByAttribute, ExpandedSectionRenderer } from 'components/common/EntityDataTable/types';
import type { ExtensionMetricFields } from 'components/streams/StreamsOverview/metricColumns';

const entityName = 'stream';
const streamOverviewTableElementsExport = 'components.streams.overview.tableElements';

export type ExtensionColumnGroups = {
  routing: Array<string>;
  performance: Array<string>;
};

const useStreamsOverviewExtensions = (): {
  columnRenderers: ColumnRenderersByAttribute<Stream>;
  attributes: {
    attributeNames: Array<string>;
    attributes: Array<Attribute>;
  };
  columnGroups: ExtensionColumnGroups;
  expandedSections: { [sectionName: string]: ExpandedSectionRenderer<Stream> };
  metricFields: ExtensionMetricFields;
} => {
  const { pluggableColumnRenderers, pluggableAttributes, pluggableExpandedSections } =
    usePluggableEntityTableElements<Stream>(null, entityName);
  const pluginTableElements = usePluginEntities(streamOverviewTableElementsExport);

  return {
    // Stream overview extensions are stream-specific and should be applied
    // before generic entity table extensions so generic plugins can still override them.
    columnRenderers: {
      ...pluginTableElements.reduce((acc, curr) => ({ ...acc, ...curr.columnRenderers }), {}),
      ...pluggableColumnRenderers,
    },
    attributes: {
      attributeNames: [
        ...pluginTableElements.map(({ attributeName }) => attributeName),
        ...pluggableAttributes.attributeNames,
      ],
      attributes: [...pluginTableElements.flatMap(({ attributes }) => attributes), ...pluggableAttributes.attributes],
    },
    columnGroups: pluginTableElements.reduce<ExtensionColumnGroups>(
      (acc, curr) => {
        const group: 'routing' | 'performance' | undefined = curr.group;

        if (group !== 'routing' && group !== 'performance') {
          return acc;
        }

        const ids = (curr.attributes ?? []).map(({ id }: { id: string }) => id);

        return { ...acc, [group]: [...acc[group], ...ids] };
      },
      { routing: [], performance: [] },
    ),
    expandedSections: pluggableExpandedSections,
    // Optional per-plugin map of column id → backend metric fields. Lets enterprise plug
    // columns like `failure_count` into the open-source `/streams/metrics` request.
    metricFields: pluginTableElements.reduce<ExtensionMetricFields>(
      (acc, curr) => ({ ...acc, ...(curr.metricFields ?? {}) }),
      {},
    ),
  };
};

export default useStreamsOverviewExtensions;
