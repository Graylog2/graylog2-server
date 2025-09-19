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
import styled from 'styled-components';

import { ListGroup, ListGroupItem } from 'components/bootstrap';
import type { ValueGroupItem, ValueGroups, FieldData } from 'views/components/visualizations/OnClickPopover/Types';
import ValueRenderer from 'views/components/visualizations/OnClickPopover/ValueRenderer';

type Props = ValueGroups & { setFieldData: React.Dispatch<React.SetStateAction<FieldData>> };

const StyledListGroup = styled(ListGroup)`
  max-height: 300px;
  overflow-y: auto;
`;

const Group = ({
  group,
  keyPrefix,
  setFieldData,
}: {
  group: Array<ValueGroupItem>;
  keyPrefix: string;
  setFieldData: Props['setFieldData'];
}) => {
  if (!group?.length) return null;

  return (
    <>
      {group.map(({ text, value, field, traceColor }) => (
        <ListGroupItem onClick={() => setFieldData({ value, field })} key={`${keyPrefix}-${value}-${field}`}>
          <ValueRenderer value={text} label={field} traceColor={traceColor} />
        </ListGroupItem>
      ))}
    </>
  );
};
const OnClickPopoverValueGroups = ({ metricValue, rowPivotValues, columnPivotValues, setFieldData }: Props) => (
  <StyledListGroup>
    {metricValue && <Group group={[metricValue]} keyPrefix="metricValue" setFieldData={setFieldData} />}
    <Group group={rowPivotValues} keyPrefix="rowPivotValues" setFieldData={setFieldData} />
    <Group group={columnPivotValues} keyPrefix="columnPivotValues" setFieldData={setFieldData} />
  </StyledListGroup>
);

export default OnClickPopoverValueGroups;
