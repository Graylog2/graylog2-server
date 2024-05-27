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
import { styled, css } from 'styled-components';

import type SeriesUnit from 'views/logic/aggregationbuilder/SeriesUnit';
import UnitMetricPopover from 'views/components/aggregationwizard/metric/UnitMetricPopover';
import UnitContainer from 'views/components/aggregationwizard/metric/UnitContainer';
import useFieldUnitTypes from 'hooks/useFieldUnitTypes';

type Props = {
  index: number,
  predefinedValue: SeriesUnit
}

const Label = styled.span(({ theme }) => css`
  color: ${theme.colors.gray[60]};
  font-weight: bold;
`);

const MetricUnit = ({ index, predefinedValue }: Props) => {
  const { getUnitInfo } = useFieldUnitTypes();

  if (predefinedValue?.isDefined) return <UnitContainer><Label title={getUnitInfo(predefinedValue.unitType, predefinedValue.abbrev).name}>{predefinedValue.abbrev}</Label></UnitContainer>;

  return <UnitMetricPopover index={index} />;
};

export default MetricUnit;
