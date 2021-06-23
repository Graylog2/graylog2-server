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
import { useCallback } from 'react';
import { useFormikContext, FieldArray, Field } from 'formik';
import styled from 'styled-components';

import { HoverForHelp, SortableList } from 'components/common';
import { Checkbox } from 'components/graylog';

import ElementConfigurationContainer from './ElementConfigurationContainer';
import GroupBy from './GroupBy';

import GroupByElement from '../aggregationElements/GroupByElement';
import { WidgetConfigFormValues } from '../WidgetConfigForm';

const RollupColumnsCheckbox = styled(Checkbox)`
  &.checkbox {
    padding-top: 0;
  }
`;

const RollupColumnsLabel = styled.div`
  display: flex;
  align-items: center;
`;

const RollupHoverForHelp = styled(HoverForHelp)`
  margin-left: 5px;
`;

const GroupByConfiguration = () => {
  const { values: { groupBy }, values, setValues, setFieldValue } = useFormikContext<WidgetConfigFormValues>();
  const disableColumnRollup = !groupBy?.groupings?.find(({ direction }) => direction === 'column');
  const removeGrouping = useCallback((index) => () => {
    setValues(GroupByElement.onRemove(index, values));
  }, [setValues, values]);

  const isEmpty = !groupBy?.groupings;

  return (
    <>
      {!isEmpty && (
        <Field name="groupBy.columnRollup">
          {({ field: { name, onChange, value } }) => (
            <RollupColumnsCheckbox onChange={() => onChange({ target: { name, value: !groupBy?.columnRollup } })}
                                   checked={value}
                                   disabled={disableColumnRollup}>
              <RollupColumnsLabel>
                Rollup Columns
                <RollupHoverForHelp title="Rollup Columns">
                  When rollup is enabled, an additional trace totalling individual subtraces will be included.
                </RollupHoverForHelp>
              </RollupColumnsLabel>
            </RollupColumnsCheckbox>
          )}
        </Field>
      )}
      <FieldArray name="groupBy.groupings"
                  validateOnChange={false}
                  render={() => (
                    <SortableList items={groupBy?.groupings}
                                  onMoveItem={(newGroupings) => setFieldValue('groupBy.groupings', newGroupings)}
                                  customListItemRender={({ item, index, dragHandleProps, draggableProps, className, ref }) => (
                                    <ElementConfigurationContainer key={`grouping-${item.id}`}
                                                                   dragHandleProps={dragHandleProps}
                                                                   draggableProps={draggableProps}
                                                                   className={className}
                                                                   testIdPrefix={`grouping-${index}`}
                                                                   onRemove={removeGrouping(index)}
                                                                   elementTitle={GroupByElement.title}
                                                                   ref={ref}>
                                      <GroupBy index={index} />
                                    </ElementConfigurationContainer>
                                  )} />
                  )} />

    </>
  );
};

export default GroupByConfiguration;
