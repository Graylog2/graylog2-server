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
import styled, { css } from 'styled-components';

import { HoverForHelp, SortableList, FormikFormGroup } from 'components/common';
import { Checkbox } from 'components/bootstrap';

import GroupingConfiguration from './GroupingConfiguration';
import GroupingElement from './GroupingElement';

import ElementConfigurationContainer from '../ElementConfigurationContainer';
import type { WidgetConfigFormValues } from '../WidgetConfigForm';

const RollupColumnsCheckbox = styled(Checkbox)`
  &.checkbox {
    padding-top: 0;
  }
`;

const RollupColumnsLabel = styled.div`
  display: flex;
  align-items: center;
`;

const RollupHoverForHelp = styled((props) => <HoverForHelp {...props} />)`
  margin-left: 5px;
`;

type GroupingsItemProps = Omit<React.ComponentProps<typeof ElementConfigurationContainer>, 'testIdPrefix' | 'onRemove' | 'elementTitle' | 'children'> & {
  /* eslint-disable react/no-unused-prop-types */
  item: { id: string },
  index: number,
  /* eslint-enable react/no-unused-prop-types */
};

const SettingsSeparator = styled.hr(({ theme }) => css`
  border-style: dashed;
  border-color: ${theme.colors.variant.lighter.default};
  margin-top: 5px;
  margin-bottom: 5px;
`);

const GroupingsConfiguration = () => {
  const { values: { groupBy }, values, setValues, setFieldValue } = useFormikContext<WidgetConfigFormValues>();
  const disableColumnRollup = !groupBy?.groupings?.find(({ direction }) => direction === 'column');
  const removeGrouping = useCallback((index) => () => {
    setValues(GroupingElement.onRemove(index, values));
  }, [setValues, values]);

  const isEmpty = !groupBy?.groupings;

  const hasValuesRowPivots = groupBy?.groupings?.find(({ direction, field }) => (direction === 'row' && field?.type === 'values')) !== undefined;
  const hasValuesColumnPivots = groupBy?.groupings?.find(({ direction, field }) => (direction === 'column' && field?.type === 'values')) !== undefined;

  const GroupingsItem = useCallback(({ item, index, dragHandleProps, draggableProps, className, ref }: GroupingsItemProps) => (
    <ElementConfigurationContainer key={`grouping-${item.id}`}
                                   dragHandleProps={dragHandleProps}
                                   draggableProps={draggableProps}
                                   className={className}
                                   testIdPrefix={`grouping-${index}`}
                                   onRemove={removeGrouping(index)}
                                   elementTitle={GroupingElement.title}
                                   ref={ref}>
      <GroupingConfiguration index={index} />
    </ElementConfigurationContainer>
  ), [removeGrouping]);

  return (
    <>
      <FieldArray name="groupBy.groupings"
                  validateOnChange={false}
                  render={() => (
                    <SortableList items={groupBy?.groupings}
                                  onMoveItem={(newGroupings) => setFieldValue('groupBy.groupings', newGroupings)}
                                  customListItemRender={GroupingsItem} />
                  )} />
      {!isEmpty && (
        <>
          <SettingsSeparator />
          <ElementConfigurationContainer elementTitle="Settings">
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
            {hasValuesRowPivots && (
            <FormikFormGroup label="Row Limit"
                             name="groupBy.rowLimit"
                             type="number"
                             bsSize="small" />
            )}
            {hasValuesColumnPivots && (
            <FormikFormGroup label="Column Limit"
                             name="groupBy.columnLimit"
                             type="number"
                             bsSize="small" />
            )}
          </ElementConfigurationContainer>
        </>
      )}
    </>
  );
};

export default GroupingsConfiguration;
