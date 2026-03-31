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

import usePluggableEntityTableElements from 'hooks/usePluggableEntityTableElements';
import type { Stream } from 'stores/streams/StreamsStore';
import type { Attribute } from 'stores/PaginationTypes';
import type { ColumnRenderersByAttribute, ExpandedSectionRenderer } from 'components/common/EntityDataTable/types';

import usePluggableStreamsOverviewTableElements from './usePluggableStreamsOverviewTableElements';

const entityName = 'stream';

const useStreamsOverviewExtensions = (): {
  columnRenderers: ColumnRenderersByAttribute<Stream>;
  attributes: {
    attributeNames: Array<string>;
    attributes: Array<Attribute>;
  };
  expandedSections: { [sectionName: string]: ExpandedSectionRenderer<Stream> };
} => {
  const {
    pluggableColumnRenderers,
    pluggableAttributes,
    pluggableExpandedSections,
  } = usePluggableEntityTableElements<Stream>(null, entityName);
  const {
    pluggableColumnRenderers: streamOverviewColumnRenderers,
    pluggableAttributes: streamOverviewAttributes,
  } = usePluggableStreamsOverviewTableElements();

  return useMemo(
    () => ({
      // Stream overview extensions are stream-specific and should be applied
      // before generic entity table extensions so generic plugins can still override them.
      columnRenderers: {
        ...streamOverviewColumnRenderers,
        ...pluggableColumnRenderers,
      },
      attributes: {
        attributeNames: [
          ...(streamOverviewAttributes.attributeNames || []),
          ...(pluggableAttributes.attributeNames || []),
        ],
        attributes: [
          ...(streamOverviewAttributes.attributes || []),
          ...(pluggableAttributes.attributes || []),
        ],
      },
      expandedSections: pluggableExpandedSections,
    }),
    [pluggableAttributes, pluggableColumnRenderers, pluggableExpandedSections, streamOverviewAttributes, streamOverviewColumnRenderers],
  );
};

export default useStreamsOverviewExtensions;
