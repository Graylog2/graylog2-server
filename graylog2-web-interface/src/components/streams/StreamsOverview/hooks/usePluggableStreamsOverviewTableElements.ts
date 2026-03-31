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
import { useMemo } from 'react';

import usePluginEntities from 'hooks/usePluginEntities';
import type { ColumnRenderersByAttribute } from 'components/common/EntityDataTable/types';
import type { Stream } from 'stores/streams/StreamsStore';
import type { Attribute } from 'stores/PaginationTypes';

const usePluggableStreamsOverviewTableElements = (): {
  pluggableColumnRenderers: ColumnRenderersByAttribute<Stream>;
  pluggableAttributes: {
    attributeNames: Array<string>;
    attributes: Array<Attribute>;
  };
} => {
  const pluginTableElements = usePluginEntities('components.streams.overview.tableElements');

  return useMemo(
    () => ({
      pluggableColumnRenderers: pluginTableElements.reduce(
        (acc, curr) => ({ ...acc, ...curr.columnRenderers }),
        {},
      ),
      pluggableAttributes: {
        attributeNames: pluginTableElements.map(({ attributeName }) => attributeName),
        attributes: pluginTableElements.flatMap(({ attributes }) => attributes),
      },
    }),
    [pluginTableElements],
  );
};

export default usePluggableStreamsOverviewTableElements;
