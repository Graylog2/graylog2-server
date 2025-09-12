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
import styled, { css } from 'styled-components';

import Value from 'views/components/Value';
import type { ValueGroupItem, ValueGroups } from 'views/components/visualizations/OnClickPopover/Types';
import ValueRenderer from 'views/components/visualizations/OnClickPopover/ValueRenderer';

type Props = ValueGroups;

const DivContainer = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xxs};
  `,
);

const Group = ({ group, keyPrefix }: { group: Array<ValueGroupItem>; keyPrefix: string }) => {
  if (!group?.length) return null;

  return group.map(({ text, value, field, traceColor }) => (
    <Value
      key={`${keyPrefix}-${value}-${field}`}
      field={field}
      value={value}
      render={() => <ValueRenderer value={text} label={field} traceColor={traceColor} />}
      actionMenuWithinPortal={false}
    />
  ));
};
const OnClickPopoverValueGroups = ({ metricValue, rowPivotValues, columnPivotValues }: Props) => (
  <DivContainer>
    {metricValue && <Group group={[metricValue]} keyPrefix="metricValue" />}
    <Group group={rowPivotValues} keyPrefix="rowPivotValues" />
    <Group group={columnPivotValues} keyPrefix="columnPivotValues" />
  </DivContainer>
);

export default OnClickPopoverValueGroups;
