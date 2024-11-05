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
import React, { useMemo } from 'react';
import { styled } from 'styled-components';

import useCurrentUser from 'hooks/useCurrentUser';
import CustomColumnRenderers from 'components/events/ColumnRenderers';
import { Table } from 'components/bootstrap';
import type { Event } from 'components/events/events/types';
import type { EventsAdditionalData } from 'components/events/fetchEvents';

const TD = styled.td`
    white-space: nowrap;
`;

type Props = {
  attributesList: Array<{ id: string, title: string}>,
  event: Event,
  meta: EventsAdditionalData,
}

const EventDetailsTable = ({ event, attributesList, meta }: Props) => {
  const currentUser = useCurrentUser();

  const { attributes: attributesRenderers } = useMemo(() => CustomColumnRenderers(currentUser.permissions), [currentUser.permissions]);

  return (
    <Table condensed striped>
      <tbody>
        {attributesList.map((attribute) => {
          const renderCell = attributesRenderers[attribute.id]?.renderCell;
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
};

export default EventDetailsTable;
