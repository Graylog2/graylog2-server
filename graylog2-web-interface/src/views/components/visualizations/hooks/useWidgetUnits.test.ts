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
import { renderHook } from 'wrappedTestingLibrary/hooks';

import { asMock } from 'helpers/mocking';
import useFeature from 'hooks/useFeature';
import useFieldTypesUnits from 'views/hooks/useFieldTypesUnits';
import FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import UnitsConfig from 'views/logic/aggregationbuilder/UnitsConfig';
import useWidgetUnits from 'views/components/visualizations/hooks/useWidgetUnits';
import Pivot from 'views/logic/aggregationbuilder/Pivot';

jest.mock('views/hooks/useFieldTypesUnits', () => jest.fn());
jest.mock('hooks/useFeature', () => jest.fn());

describe('useFieldTypeUnits', () => {
  const timeMs = FieldUnit.fromJSON({ abbrev: 'ms', unit_type: 'time' });
  const timeH = FieldUnit.fromJSON({ abbrev: 'h', unit_type: 'time' });
  const sizeB = FieldUnit.fromJSON({ abbrev: 'b', unit_type: 'size' });
  const sizeMb = FieldUnit.fromJSON({ abbrev: 'Mb', unit_type: 'size' });
  const percent = FieldUnit.fromJSON({ abbrev: '%', unit_type: 'percent' });
  const units: UnitsConfig = UnitsConfig
    .empty().toBuilder()
    .setFieldUnit('fieldTime', timeH)
    .setFieldUnit('fieldSizeMb', sizeMb)
    .setFieldUnit('fieldPercent', percent)
    .build();
  const testConfig: AggregationWidgetConfig = AggregationWidgetConfig.builder().series([
    Series.create('avg', 'fieldTime')
      .toBuilder()
      .config(SeriesConfig.empty().toBuilder().name('Name1').build()).build(),
  ]).units(units)
    .rowPivots([Pivot.createValues(['fieldSizeMb', 'fieldSize'])])
    .columnPivots([Pivot.createValues(['fieldPercent'])])
    .build();

  beforeEach(() => {
    asMock(useFeature).mockReturnValue(true);

    asMock(useFieldTypesUnits).mockImplementation(() => (
      {
        fieldTime: timeMs,
        fieldSize: sizeB,
        filedNotInConfiguration: timeMs,
      }
    ));
  });

  it('Creates unit config from config and predefined values only for values from pivots and metrics with priority for config values', () => {
    const { result } = renderHook(() => useWidgetUnits(testConfig));

    expect(result.current).toEqual(new UnitsConfig({
      fieldTime: timeH,
      fieldSizeMb: sizeMb,
      fieldSize: sizeB,
      fieldPercent: percent,
    }));
  });
});
