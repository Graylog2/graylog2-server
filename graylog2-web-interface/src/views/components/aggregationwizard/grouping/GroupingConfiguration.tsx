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
import { useFormikContext } from 'formik';

import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';
import { FormikFormGroup } from 'components/common';
import { DateType, ValuesType } from 'views/logic/aggregationbuilder/Pivot';

import Direction from './configuration/Direction';
import FieldComponent from './configuration/FieldComponent';
import Time from './configuration/Time';

const Wrapper = styled.div``;

type Props = {
  index: number,
}

const GroupingConfiguration = React.memo(({ index }: Props) => {
  const { values: { groupBy } } = useFormikContext<WidgetConfigFormValues>();
  const fieldType = groupBy.groupings[index].type;

  return (
    <Wrapper data-testid={`grouping-${index}`}>
      <Direction groupingIndex={index} />
      <FieldComponent groupingIndex={index} />
      {fieldType === DateType && (<Time index={index} />)}
      {fieldType === ValuesType && (
        <FormikFormGroup label="Limit"
                         name={`groupBy.groupings.${index}.limit`}
                         type="number"
                         bsSize="small" />
      )}
    </Wrapper>
  );
});

export default GroupingConfiguration;
