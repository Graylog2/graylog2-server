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
import isString from 'lodash/isString';

import { HoverForHelp, SortableList } from 'components/common';
import { Checkbox } from 'components/bootstrap';
import FormErrors from 'components/common/FormErrors';

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

type RollupHoverForHelpProps = {
  children: React.ReactNode,
  title: string,
};

const RollupHoverForHelp = styled((props: RollupHoverForHelpProps) => <HoverForHelp {...props} />)`
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
  const { values: { groupBy }, values, errors, setValues, setFieldValue } = useFormikContext<WidgetConfigFormValues>();
  const disableColumnRollup = !groupBy?.groupings?.find(({ direction }) => direction === 'column');
  const removeGrouping = useCallback((index) => () => {
    setValues(GroupingElement.onRemove(index, values));
  }, [setValues, values]);

  const isEmpty = (groupBy?.groupings ?? []).length === 0;

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

  const hasGroupByError = isString(errors?.groupBy);

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
                                       checked={value ?? false}
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
          </ElementConfigurationContainer>
        </>
      )}
      {hasGroupByError && <FormErrors errors={{ groupBy: errors?.groupBy as string }} />}
    </>
  );
};

export default GroupingsConfiguration;
