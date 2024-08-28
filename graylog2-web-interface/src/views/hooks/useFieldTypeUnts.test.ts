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
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType, { Properties } from 'views/logic/fieldtypes/FieldType';
import FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';

jest.mock('hooks/useFeature');
jest.mock('views/logic/fieldtypes/useFieldTypes', () => jest.fn());

describe('useFieldTypeUnits', () => {
  const time = FieldUnit.fromJSON({ abbrev: 'ms', unit_type: 'time' });
  const size = FieldUnit.fromJSON({ abbrev: 'b', unit_type: 'size' });
  const percent = FieldUnit.fromJSON({ abbrev: '%', unit_type: 'percent' });
  const fieldTypes = [
    FieldTypeMapping.create('fieldTime',
      FieldType.create('number', [Properties.Numeric]),
      time),

    FieldTypeMapping.create('fieldSize',
      FieldType.create('number', [Properties.Numeric]),
      size),

    FieldTypeMapping.create('fieldPercent',
      FieldType.create('number', [Properties.Numeric]),
      percent),

    FieldTypeMapping.create('noUnitField',
      FieldType.create('number', [Properties.Numeric])),
  ];

  beforeEach(() => {
    asMock(useFeature).mockReturnValue(true);

    asMock(useFieldTypes).mockImplementation(() => ({
      data: fieldTypes, isLoading: false, isFetching: false, refetch: () => {},
    }));
  });

  it('return correct mapping', () => {
    const { result } = renderHook(() => useFieldTypesUnits());

    expect(result.current).toEqual({
      fieldTime: time,
      fieldSize: size,
      fieldPercent: percent,
    });
  });
});
