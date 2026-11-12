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

import type { Event, EventsAdditionalData } from 'components/events/events/types';
import type { ColumnRenderersByAttribute, EntityBase } from 'components/common/EntityDataTable/types';
import ColumnRenderers, { FieldsRenderer, GroupByFieldsRenderer } from 'components/events/events/ColumnRenderers';

// The details list all fields of an event, while a table cell may show them in a more compact way.
export const getEventFieldsDetailsRenderers = <T extends EntityBase, M = unknown>(): ColumnRenderersByAttribute<
  T,
  M
> => ({
  fields: {
    renderCell: (fields: Record<string, string>) => <FieldsRenderer fields={fields} />,
  },
  group_by_fields: {
    renderCell: (groupByFields: Record<string, string>) => <GroupByFieldsRenderer groupByFields={groupByFields} />,
  },
});

/**
 * Renderers for the attributes in the event details. They reuse the renderers of the table columns,
 * except for attributes the details show differently.
 */
const EventDetailsAttributeRenderers: ColumnRenderersByAttribute<Event, EventsAdditionalData> = {
  ...ColumnRenderers.attributes,
  ...getEventFieldsDetailsRenderers<Event, EventsAdditionalData>(),
};

export default EventDetailsAttributeRenderers;
