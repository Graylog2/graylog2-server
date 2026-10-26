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
import type { Row } from '@tanstack/react-table';
import { createColumnHelper } from '@tanstack/react-table';
import * as React from 'react';
import { useMemo } from 'react';
import styled from 'styled-components';

import { Button } from 'components/bootstrap';
import Icon from 'components/common/Icon';
import type { EntityBase } from 'components/common/EntityDataTable/types';
import { ROW_EXPAND_COL_ID } from 'components/common/EntityDataTable/Constants';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';

const Container = styled.div`
  display: flex;
  justify-content: center;
`;

const ToggleButton = styled(Button)`
  && {
    aspect-ratio: 1;
    padding: 0;
  }
`;

// Without a block box the glyph sits on the inline line box of the button and ends up off-center.
const ToggleIcon = styled(Icon)`
  display: block;
  line-height: 1;
`;

const RowExpandCell = <Entity extends EntityBase>({ row }: { row: Row<Entity> }) => {
  const { expandedSections, toggleRow } = useExpandedSections();
  const isExpanded = !!expandedSections?.[row.id];
  const title = isExpanded ? 'Collapse row' : 'Expand row';

  return (
    <Container>
      <ToggleButton bsStyle="default" bsSize="xs" onClick={() => toggleRow(row.id)} title={title} aria-label={title}>
        <ToggleIcon name={isExpanded ? 'keyboard_arrow_up' : 'keyboard_arrow_down'} />
      </ToggleButton>
    </Container>
  );
};

const useRowExpandColumnDefinition = <Entity extends EntityBase>(displayRowExpandCol: boolean, colWidth: number) => {
  const columnHelper = createColumnHelper<Entity>();

  return useMemo(
    () =>
      displayRowExpandCol
        ? columnHelper.display({
            id: ROW_EXPAND_COL_ID,
            size: colWidth,
            enableHiding: false,
            cell: RowExpandCell<Entity>,
            enableResizing: false,
          })
        : null,
    [displayRowExpandCol, columnHelper, colWidth],
  );
};

export default useRowExpandColumnDefinition;
