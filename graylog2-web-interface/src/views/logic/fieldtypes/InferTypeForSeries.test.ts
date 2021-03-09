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
import inferTypeForSeries from './InferTypeForSeries';
import FieldTypeMapping from './FieldTypeMapping';
import FieldType, { FieldTypes } from './FieldType';

import Series from '../aggregationbuilder/Series';

describe('InferTypeForSeries', () => {
  it.each`
    func              | expectedType          | field
    ${'card'}         | ${FieldTypes.LONG()}  | ${'foo'}
    ${'count'}        | ${FieldTypes.LONG()}  | ${'foo'} 
    ${'stddev'}       | ${FieldTypes.FLOAT()} | ${'foo'} 
    ${'sum'}          | ${FieldTypes.FLOAT()} | ${'foo'} 
    ${'sum'}          | ${FieldTypes.FLOAT()} | ${'foo-bar'} 
    ${'sumofsquares'} | ${FieldTypes.FLOAT()} | ${'foo'} 
  `('returns expected type for constant type function "$func($field)"', ({ func, expectedType, field }) => {
    const functionName = `${func}(${field})`;

    expect(inferTypeForSeries(Series.forFunction(functionName), []))
      .toEqual(FieldTypeMapping.create(functionName, expectedType));
  });

  it.each`
    func     | fieldType
    ${'avg'} | ${FieldTypes.DATE()}
    ${'min'} | ${FieldTypes.DATE()}
    ${'max'} | ${FieldTypes.DATE()}
    ${'avg'} | ${FieldTypes.LONG()}
    ${'min'} | ${FieldTypes.LONG()}
    ${'max'} | ${FieldTypes.LONG()}
  `('returns type of field for "$func(field)"', ({ func, fieldType }) => {
    const fieldName = 'bar';
    const types = [FieldTypeMapping.create(fieldName, fieldType)];

    const functionName = `${func}(${fieldName})`;

    expect(inferTypeForSeries(Series.forFunction(functionName), types))
      .toEqual(FieldTypeMapping.create(functionName, fieldType));
  });

  it('returns unknown if field type is not present', () => {
    expect(inferTypeForSeries(Series.forFunction('avg(foo)'), []))
      .toEqual(FieldTypeMapping.create('avg(foo)', FieldType.Unknown));
  });

  it('returns unknown if field types are `undefined`', () => {
    expect(inferTypeForSeries(Series.forFunction('avg(foo)'), undefined))
      .toEqual(FieldTypeMapping.create('avg(foo)', FieldType.Unknown));
  });
});
