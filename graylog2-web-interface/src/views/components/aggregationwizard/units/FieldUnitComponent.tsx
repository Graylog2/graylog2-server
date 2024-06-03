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
import React, { useEffect } from 'react';
import { styled, css } from 'styled-components';
import { useFormikContext } from 'formik';

import type FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import FieldUnitPopover from 'views/components/aggregationwizard/units/FieldUnitPopover';
import UnitContainer from 'views/components/aggregationwizard/units/UnitContainer';
import useFieldUnitTypes from 'hooks/useFieldUnitTypes';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';

type Props = {
  field: string,
  predefinedValue: FieldUnit
}

const Label = styled.span(({ theme }) => css`
  color: ${theme.colors.gray[60]};
  font-weight: bold;
`);

const FieldUnitComponent = ({ field, predefinedValue }: Props) => {
  const { getUnitInfo } = useFieldUnitTypes();
  const { setFieldValue } = useFormikContext<WidgetConfigFormValues>();

  useEffect(() => {
    if (predefinedValue) {
      setFieldValue(`units.${field}`, { abbrev: predefinedValue.abbrev, unitType: predefinedValue.unitType });
    }
  }, [predefinedValue]);

  if (predefinedValue?.isDefined) return <UnitContainer><Label title={getUnitInfo(predefinedValue.unitType, predefinedValue.abbrev).name}>{predefinedValue.abbrev}</Label></UnitContainer>;

  return <FieldUnitPopover field={field} />;
};

export default FieldUnitComponent;
