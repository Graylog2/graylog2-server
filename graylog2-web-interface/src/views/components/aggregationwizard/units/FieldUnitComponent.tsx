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
import React, { useEffect, useMemo } from 'react';
import { styled, css } from 'styled-components';
import { useFormikContext } from 'formik';

import FieldUnitPopover from 'views/components/aggregationwizard/units/FieldUnitPopover';
import { getUnitInfo } from 'views/components/visualizations/utils/unitConvertors';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';
import useFieldTypesUnits from 'views/hooks/useFieldTypesUnits';

type Props = {
  field: string,
}

export const UnitLabel = styled.div(({ theme }) => css`
  color: ${theme.colors.gray[60]};
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 25px;
  height: 25px;
`);

const FieldUnitComponent = ({ field }: Props) => {
  const fieldTypesUnits = useFieldTypesUnits();
  const predefinedValue = useMemo(() => fieldTypesUnits?.[field], [fieldTypesUnits]);
  const { setFieldValue } = useFormikContext<WidgetConfigFormValues>();

  useEffect(() => {
    if (predefinedValue) {
      setFieldValue(`units.${field}`, { abbrev: predefinedValue.abbrev, unitType: predefinedValue.unitType });
    }
  }, [predefinedValue]);

  if (predefinedValue?.isDefined) return <UnitLabel title={getUnitInfo(predefinedValue.unitType, predefinedValue.abbrev).name}>{predefinedValue.abbrev}</UnitLabel>;

  return <FieldUnitPopover field={field} />;
};

export default FieldUnitComponent;
