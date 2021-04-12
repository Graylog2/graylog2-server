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

import { HoverForHelp } from 'components/common';
import { Checkbox } from 'components/graylog';

import ElementConfigurationSection from './ElementConfigurationSection';
import GroupBy from './GroupBy';

import GroupByElement from '../aggregationElements/GroupByElement';
import { WidgetConfigFormValues } from '../WidgetConfigForm';

const RollupColumnsLabel = styled.div`
  display: flex;
  align-items: center;
`;

const RollupHoverForHelp = styled(HoverForHelp)`
  margin-left: 5px;
`;

const GroupByConfiguration = () => {
  const { values: { groupBy }, values, setValues } = useFormikContext<WidgetConfigFormValues>();
  const disableColumnRollup = !groupBy.groupings.find(({ direction }) => direction === 'column');
  const removeGrouping = useCallback((index) => {
    setValues(GroupByElement.removeElementSection(index, values));
  }, [setValues, values]);

  return (
    <>
      <Field name="groupBy.columnRollup">
        {({ field: { name, onChange, value } }) => (
          <Checkbox onChange={() => onChange({ target: { name, value: !groupBy.columnRollup } })}
                    checked={value}
                    disabled={disableColumnRollup}>
            <RollupColumnsLabel>
              Rollup Columns
              <RollupHoverForHelp title="Rollup Columns">
                When rollup is enabled, an additional trace totalling individual subtraces will be included.
              </RollupHoverForHelp>
            </RollupColumnsLabel>
          </Checkbox>
        )}
      </Field>
      <FieldArray name="groupBy.groupings"
                  render={() => (
                    <>
                      <div>
                        {groupBy.groupings.map((grouping, index) => {
                          return (
                            // eslint-disable-next-line react/no-array-index-key
                            <ElementConfigurationSection key={`grouping-${index}`} onRemove={() => removeGrouping(index)}>
                              <GroupBy index={index} />
                            </ElementConfigurationSection>
                          );
                        })}
                      </div>
                    </>
                  )} />
    </>
  );
};

export default GroupByConfiguration;
