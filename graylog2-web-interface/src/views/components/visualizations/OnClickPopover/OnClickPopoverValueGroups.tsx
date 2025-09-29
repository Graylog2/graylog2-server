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

import { ListGroup, ListGroupItem } from 'components/bootstrap';
import type { ValueGroupItem, ValueGroups, FieldData } from 'views/components/visualizations/OnClickPopover/Types';
import ValueRenderer from 'views/components/visualizations/OnClickPopover/ValueRenderer';
import { humanSeparator } from 'views/Constants';

type Props = ValueGroups & { setFieldData: React.Dispatch<React.SetStateAction<FieldData>> };

const StyledListGroup = styled(ListGroup)`
  max-height: 300px;
  overflow-y: auto;
`;

const GroupingsContainer = styled.div<{ $withMargin: boolean }>(
  ({ theme, $withMargin }) => css`
    margin-left: ${$withMargin ? theme.spacings.sm : 0};
  `,
);

const Label = styled.div(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.xxs};
    font-size: ${theme.fonts.size.small};
    font-weight: bold;
    color: ${theme.colors.text.secondary};
  `,
);

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
        <ListGroupItem
          onClick={() => setFieldData({ value, field, contexts: null })}
          key={`${keyPrefix}-${value}-${field}`}>
          <ValueRenderer value={text} label={field} traceColor={traceColor} />
        </ListGroupItem>
      ))}
    </>
  );
};

type GroupingActionsProps = {
  columnPivotValues: Array<ValueGroupItem>;
  rowPivotValues: Array<ValueGroupItem>;
  setFieldData: Props['setFieldData'];
  metricValue: ValueGroupItem;
};

const GroupingActions = ({ columnPivotValues, rowPivotValues, setFieldData, metricValue }: GroupingActionsProps) => {
  const valuePath = [...columnPivotValues, ...rowPivotValues].map(({ value, field }) => ({ [field]: value }));

  return (
    <ListGroupItem
      onClick={() =>
        setFieldData({
          value: metricValue.value,
          field: metricValue.field,
          contexts: { valuePath },
        })
      }>
      <ValueRenderer
        label=""
        value={valuePath.map((o) => Object.values(o)[0]).join(humanSeparator)}
        traceColor={metricValue.traceColor}
      />
    </ListGroupItem>
  );
};

const OnClickPopoverValueGroups = ({ metricValue, rowPivotValues, columnPivotValues, setFieldData }: Props) => {
  const showMultipleAction = (rowPivotValues.length ?? 0) + (columnPivotValues.length ?? 0) > 1;

  return (
    <StyledListGroup>
      <Label>Metric</Label>
      {metricValue && <Group group={[metricValue]} keyPrefix="metricValue" setFieldData={setFieldData} />}
      <Label>Groupings</Label>
      {showMultipleAction && (
        <GroupingActions
          columnPivotValues={columnPivotValues}
          rowPivotValues={rowPivotValues}
          setFieldData={setFieldData}
          metricValue={metricValue}
        />
      )}
      <GroupingsContainer $withMargin={showMultipleAction}>
        <Group group={rowPivotValues} keyPrefix="rowPivotValues" setFieldData={setFieldData} />
        <Group group={columnPivotValues} keyPrefix="columnPivotValues" setFieldData={setFieldData} />
      </GroupingsContainer>
    </StyledListGroup>
  );
};

export default OnClickPopoverValueGroups;
