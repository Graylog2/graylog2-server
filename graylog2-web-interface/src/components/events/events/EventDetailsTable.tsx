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
import { styled } from 'styled-components';

import { Table } from 'components/bootstrap';
import type { Event, EventsAdditionalData } from 'components/events/events/types';
import type { ColumnRenderersByAttribute, EntityBase } from 'components/common/EntityDataTable/types';
import DefaultColumnRenderers from 'components/common/EntityDataTable/DefaultColumnRenderers';
import type { Attribute } from 'stores/PaginationTypes';

const TD = styled.td`
  white-space: nowrap;
`;

type Props<T extends EntityBase, M = EventsAdditionalData> = {
  attributesList: Array<{ id: string, title: string}>,
  event: T,
  meta?: M | {},
  attributesRenderers: ColumnRenderersByAttribute<T, M>
}

const EventDetailsTable = <E extends EntityBase = Event>({ event, attributesList, meta = {}, attributesRenderers }: Props<E>) => (
  <Table condensed striped>
    <tbody>
      {attributesList.map((attribute: Attribute) => {
        const defaultTypeRenderer = DefaultColumnRenderers.types?.[attribute?.type]?.renderCell;
        const typeRenderer = attributesRenderers?.types?.[attribute?.type]?.renderCell;
        const renderCell = attributesRenderers[attribute.id]?.renderCell ?? typeRenderer ?? defaultTypeRenderer;
        const value = event[attribute.id];

        return (
          <tr key={attribute.id}>
            <TD><b>{attribute.title}</b></TD>
            <td>{renderCell ? renderCell(value, event, attribute, meta) : value}</td>
          </tr>
        );
      })}
    </tbody>
  </Table>
);

export default EventDetailsTable;
