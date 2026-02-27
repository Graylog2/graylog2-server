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
import getFieldNameFromTrace from 'views/components/visualizations/utils/getFieldNameFromTrace';
import Series from 'views/logic/aggregationbuilder/Series';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';

describe('getFieldNameFromTrace', () => {
  const series = [
    Series.create('avg', 'field1'),
    Series.create('avg', 'field2')
      .toBuilder()
      .config(SeriesConfig.empty().toBuilder().name('SeriesName').build())
      .build(),
  ];

  it('shows field name when series doesnt have a name and column has only function(field) in name', () => {
    const result = getFieldNameFromTrace({ series, fullPath: 'avg(field1)' });

    expect(result).toEqual('field1');
  });

  it('shows field name when series doesnt have a name and column has only somePath-function(field) in name', () => {
    const result = getFieldNameFromTrace({ series, fullPath: 'field5Value-field6Value-avg(field1)' });

    expect(result).toEqual('field1');
  });

  it('shows field name when series has a name and column has only function(field) in name', () => {
    const result = getFieldNameFromTrace({ series, fullPath: 'SeriesName' });

    expect(result).toEqual('field2');
  });

  it('shows field name when series has a name and column has only somePath-function(field) in name', () => {
    const result = getFieldNameFromTrace({ series, fullPath: 'field5Value-field6Value-SeriesName' });

    expect(result).toEqual('field2');
  });

  it('return null when no series found', () => {
    const result = getFieldNameFromTrace({ series, fullPath: 'field5Value-field6Value-SeriesName2' });

    expect(result).toEqual(null);
  });
});
