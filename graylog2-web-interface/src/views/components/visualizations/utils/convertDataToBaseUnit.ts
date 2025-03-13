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
import type { ConversionParams } from 'views/components/visualizations/utils/unitConverters';
import type FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import { convertValueToBaseUnit } from 'views/components/visualizations/utils/unitConverters';

const convertDataToBaseUnit = (values: Array<number>, unit: FieldUnit) => {
  if (!unit?.isDefined) return values;
  const from: ConversionParams = { abbrev: unit.abbrev, unitType: unit.unitType };

  return values.map((v) => {
    const converted = convertValueToBaseUnit(v, from);

    return converted.value;
  });
};

export default convertDataToBaseUnit;
