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
import * as React from 'react';
import isEmpty from 'lodash/isEmpty';

import type { Event, EventsAdditionalData } from 'components/events/events/types';
import type { ColumnRenderersByAttribute, EntityBase } from 'components/common/EntityDataTable/types';
import ColumnRenderers from 'components/events/events/ColumnRenderers';
import EventFields from 'components/events/events/EventFields';
import ChipsCell from 'components/common/ChipsCell';
import useAppendTagFilter from 'components/events/useAppendTagFilter';

const TagsDetails = ({ tags, interactive }: { tags: ReadonlyArray<string> | undefined; interactive: boolean }) => {
  const onTagClick = useAppendTagFilter();

  if (!tags?.length) {
    return <em>No tags</em>;
  }

  if (!interactive) {
    return <ChipsCell items={tags} truncate={false} />;
  }

  return <ChipsCell items={tags} truncate={false} onItemClick={onTagClick} itemLabel="tag" />;
};

type Options = {
  // Tag chips only make sense as filter shortcuts when a filterable list is in scope.
  // Turn it off elsewhere (e.g. widgets, investigation evidence), so clicks don't add an unused filter to the URL.
  interactiveTags?: boolean;
};

/**
 * Renderers of the attributes which the event details show differently than a table cell:
 * They list all fields and tags, while a cell shows them in a compact way. And they render the message
 * as plain text, since clicking it in the table toggles the details.
 */
export const getEventDetailsRenderers = <T extends EntityBase, M = unknown>({
  interactiveTags = true,
}: Options = {}): ColumnRenderersByAttribute<T, M> => ({
  message: {
    renderCell: (message: string) => message,
  },
  fields: {
    renderCell: (fields: Record<string, string>) =>
      isEmpty(fields) ? <em>No additional Fields added to this Event.</em> : <EventFields fields={fields} />,
  },
  group_by_fields: {
    renderCell: (groupByFields: Record<string, string>) =>
      isEmpty(groupByFields) ? <em>No group-by fields on this Event.</em> : <EventFields fields={groupByFields} />,
  },
  tags: {
    renderCell: (tags: Array<string>) => <TagsDetails tags={tags} interactive={interactiveTags} />,
  },
});

/**
 * Renderers for the attributes in the event details. They reuse the renderers of the table columns,
 * except for attributes the details show differently.
 */
const getEventDetailsAttributeRenderers = (
  options: Options = {},
): ColumnRenderersByAttribute<Event, EventsAdditionalData> => ({
  ...ColumnRenderers.attributes,
  ...getEventDetailsRenderers<Event, EventsAdditionalData>(options),
});

export default getEventDetailsAttributeRenderers;
