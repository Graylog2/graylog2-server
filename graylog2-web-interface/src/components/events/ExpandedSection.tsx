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

import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import type { Attribute } from 'stores/PaginationTypes';
import type { Event } from 'components/events/events/types';
import useCurrentUser from 'hooks/useCurrentUser';
import CustomColumnRenderers from 'components/events/ColumnRenderers';
import { keyFn } from 'components/events/events/fetchEvents';
import { Table } from 'components/bootstrap';

type Props = {
  defaultLayout: Parameters<typeof useTableLayout>[0],
  event: Event,
}

const TD = styled.td`
    white-space: nowrap;
`;

const ExpandedSection = ({ defaultLayout, event }: Props) => {
  const { layoutConfig: { displayedAttributes }, isInitialLoading } = useTableLayout(defaultLayout);
  const { attributes } = useTableFetchContext();

  const nonDisplayedAttributes = useMemo(() => {
    if (isInitialLoading) return [];

    const displayedAttributesSet = new Set(displayedAttributes);

    return attributes.filter(({ id }) => !displayedAttributesSet.has(id)).map(({ id, title }: Attribute) => ({ id, title }));
  }, [attributes, displayedAttributes, isInitialLoading]);

  const currentUser = useCurrentUser();

  const { attributes: attributesRenderers } = useMemo(() => CustomColumnRenderers(currentUser.permissions, keyFn), [currentUser.permissions]);

  return (
    <Table condensed striped>
      <tbody>
        {nonDisplayedAttributes.map((attribute) => {
          const renderCell = attributesRenderers[attribute.id]?.renderCell;
          const value = event[attribute.id];

          return (
            <tr key={attribute.id}>
              <TD><b>{attribute.title}</b></TD>
              <td>{renderCell ? renderCell(value, event, attribute) : value}</td>
            </tr>
          );
        })}
      </tbody>
    </Table>
  );
};

export default ExpandedSection;
