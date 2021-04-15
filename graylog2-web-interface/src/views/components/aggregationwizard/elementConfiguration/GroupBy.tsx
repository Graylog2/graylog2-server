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

import { FormikFormGroup } from 'components/common';
import { WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

import Direction from './GroupBy/Direction';
import FieldComponent from './GroupBy/FieldComponent';
import Time from './GroupBy/Time';

const Wrapper = styled.div``;

type Props = {
  index: number,
}

const GroupBy = ({ index }: Props) => {
  const { values: { groupBy } } = useFormikContext<WidgetConfigFormValues>();
  const fieldType = groupBy.groupings[index].field.type;

  return (
    <Wrapper>
      <Direction index={index} />
      <FieldComponent index={index} fieldType={fieldType} />
      {fieldType === 'time' && (<Time index={index} />)}
      {fieldType === 'values' && (
        <FormikFormGroup label="Limit" name={`groupBy.groupings.${index}.limit`} type="number" bsSize="small" />
      )}
    </Wrapper>
  );
};

export default GroupBy;
