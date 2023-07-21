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
import { useFormikContext, Field } from 'formik';

import { Checkbox } from 'components/bootstrap';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';
import { FormikFormGroup, HoverForHelp } from 'components/common';
import { DateType, ValuesType } from 'views/logic/aggregationbuilder/Pivot';

import Direction from './configuration/Direction';
import FieldComponent from './configuration/FieldComponent';
import Time from './configuration/Time';

const Wrapper = styled.div``;

type Props = {
  index: number,
}

type SkipEmptyValuesHoverForHelpProps = {
  children: React.ReactNode,
  title: string,
};

const SkipEmptyValuesHoverForHelp = styled((props: SkipEmptyValuesHoverForHelpProps) => <HoverForHelp {...props} />)`
  margin-left: 5px;
`;

const SkipEmptyValuesCheckbox = styled(Checkbox)`
  &.checkbox {
    padding-top: 0;
  }
`;
const SkipEmptyValuesLabel = styled.div`
  display: flex;
  align-items: center;
`;

type SkipEmptyValuesPropes = {
  index: number,
}
const SkipEmptyValues = ({ index }: SkipEmptyValuesPropes) => (
  <Field name={`groupBy.groupings.${index}.skipEmptyValues`}>
    {({ field: { name, value, onChange } }) => (
      <SkipEmptyValuesCheckbox onChange={() => onChange({ target: { name, value: !value } })} checked={value ?? false}>
        <SkipEmptyValuesLabel>
          Skip Empty Values
          <SkipEmptyValuesHoverForHelp title="Skip Empty Values">
            When this is enabled, messages which do not contain the configured fields will be skipped.
            <p />
            Otherwise an &quot;(Empty Value)&quot; bucket will be created.
          </SkipEmptyValuesHoverForHelp>
        </SkipEmptyValuesLabel>
      </SkipEmptyValuesCheckbox>
    )}
  </Field>
);

const GroupingConfiguration = React.memo(({ index }: Props) => {
  const { values: { groupBy } } = useFormikContext<WidgetConfigFormValues>();
  const fieldType = groupBy.groupings[index].type;

  return (
    <Wrapper data-testid={`grouping-${index}`}>
      <Direction groupingIndex={index} />
      <FieldComponent groupingIndex={index} />
      {fieldType === DateType && (<Time index={index} />)}
      {fieldType === ValuesType && (
        <>
          <FormikFormGroup label="Limit"
                           name={`groupBy.groupings.${index}.limit`}
                           type="number"
                           bsSize="small" />
          <SkipEmptyValues index={index} />
        </>
      )}
    </Wrapper>
  );
});

export default GroupingConfiguration;
