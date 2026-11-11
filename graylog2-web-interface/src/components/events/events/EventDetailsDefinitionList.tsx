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
import React from 'react';
import isEmpty from 'lodash/isEmpty';

import { DefinitionList } from 'components/common';
import EventFields from 'components/events/events/EventFields';
import type { Event, EventsAdditionalData } from 'components/events/events/types';
import type { ColumnRenderersByAttribute, EntityBase } from 'components/common/EntityDataTable/types';
import DefaultColumnRenderers from 'components/common/EntityDataTable/DefaultColumnRenderers';
import type { Attribute } from 'stores/PaginationTypes';

// The details have room for all fields, so they list them instead of using the collapsed list of the table cells.
const detailsRenderers: { [attributeId: string]: (value: unknown) => React.ReactNode } = {
  fields: (fields: Record<string, string>) =>
    isEmpty(fields) ? <em>No additional Fields added to this Event.</em> : <EventFields fields={fields} />,
  group_by_fields: (groupByFields: Record<string, string>) =>
    isEmpty(groupByFields) ? <em>No group-by fields on this Event.</em> : <EventFields fields={groupByFields} />,
};

type Props<T extends EntityBase, M = EventsAdditionalData> = {
  attributesList: Array<{ id: string; title: string }>;
  event: T;
  meta?: M | {};
  eventProcedureId?: string;
  attributesRenderers: ColumnRenderersByAttribute<T, M>;
};

const EventDetailsDefinitionList = <E extends EntityBase = Event>({
  event,
  attributesList,
  meta = {},
  eventProcedureId = '',
  attributesRenderers,
}: Props<E>) => (
  <DefinitionList>
    {attributesList.map((attribute: Attribute) => {
      const defaultTypeRenderer = DefaultColumnRenderers.types?.[attribute?.type]?.renderCell;
      const typeRenderer = attributesRenderers?.types?.[attribute?.type]?.renderCell;
      const renderCell = attributesRenderers[attribute.id]?.renderCell ?? typeRenderer ?? defaultTypeRenderer;
      const value = event[attribute.id];
      const detailsRenderer = detailsRenderers[attribute.id];

      return (
        <React.Fragment key={attribute.id}>
          <dt>{attribute.title}</dt>
          <dd>{detailsRenderer?.(value) ?? (renderCell ? renderCell(value, event, meta, eventProcedureId) : value)}</dd>
        </React.Fragment>
      );
    })}
  </DefinitionList>
);

export default EventDetailsDefinitionList;
