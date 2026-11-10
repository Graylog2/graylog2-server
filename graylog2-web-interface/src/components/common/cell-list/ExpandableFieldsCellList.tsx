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
import styled from 'styled-components';

import { DefinitionList } from 'components/common';
import { defaultCompare } from 'logic/DefaultCompare';
import CellListCollapsedRow from 'components/common/cell-list/CellListCollapsedRow';
import CellListExpanded from 'components/common/cell-list/CellListExpanded';
import CellListScrollArea from 'components/common/cell-list/CellListScrollArea';
import CellListToggle from 'components/common/cell-list/CellListToggle';
import useCellListToggle, { DEFAULT_COLLAPSED_COUNT } from 'components/common/cell-list/useCellListToggle';

const CollapsedFields = styled.div`
  min-width: 0;
`;

type Fields = Record<string, React.ReactNode>;

const renderFields = (fields: Fields, fieldNames: ReadonlyArray<string>) => (
  // Table rows are striped already.
  <DefinitionList striped={false}>
    {fieldNames.map((fieldName) => (
      <React.Fragment key={fieldName}>
        <dt>{fieldName}</dt>
        <dd>{fields[fieldName]}</dd>
      </React.Fragment>
    ))}
  </DefinitionList>
);

type Props = {
  fields: Fields | undefined | null;
  collapsedCount?: number;
};

/**
 * Displays fields with their values in an entity data table cell, sorted by name, as one definition list.
 * Like `ExpandableCellList`, it shows the first fields and the number of the remaining ones, which can be expanded.
 */
const ExpandableFieldsCellList = ({ fields: fieldsProp, collapsedCount = DEFAULT_COLLAPSED_COUNT }: Props) => {
  const fields = fieldsProp ?? {};
  const fieldNames = Object.keys(fields).sort(defaultCompare);
  const { isExpanded, hiddenCount, toggle } = useCellListToggle(fieldNames.length, collapsedCount);

  if (!fieldNames.length) {
    return null;
  }

  if (hiddenCount <= 0) {
    return renderFields(fields, fieldNames);
  }

  if (!isExpanded) {
    return (
      <CellListCollapsedRow>
        <CollapsedFields>{renderFields(fields, fieldNames.slice(0, collapsedCount))}</CollapsedFields>
        <CellListToggle isExpanded={false} hiddenCount={hiddenCount} onToggle={toggle} />
      </CellListCollapsedRow>
    );
  }

  return (
    <CellListExpanded>
      <CellListScrollArea>{renderFields(fields, fieldNames)}</CellListScrollArea>
      <CellListToggle isExpanded hiddenCount={hiddenCount} onToggle={toggle} />
    </CellListExpanded>
  );
};

export default ExpandableFieldsCellList;
