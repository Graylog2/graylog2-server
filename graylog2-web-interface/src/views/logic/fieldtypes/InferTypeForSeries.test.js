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
// @flow strict

// $FlowFixMe: imports from core need to be fixed in flow
import each from 'jest-each';

import inferTypeForSeries from './InferTypeForSeries';
import FieldTypeMapping from './FieldTypeMapping';
import FieldType, { FieldTypes } from './FieldType';

import Series from '../aggregationbuilder/Series';

describe('InferTypeForSeries', () => {
  each`
    func              | expectedType
    ${'card'}         | ${FieldTypes.LONG()}
    ${'count'}        | ${FieldTypes.LONG()}
    ${'stddev'}       | ${FieldTypes.FLOAT()}
    ${'sum'}          | ${FieldTypes.FLOAT()}
    ${'sumofsquares'} | ${FieldTypes.FLOAT()}
  `.test('returns expected type for constant type function "$func(field)"', ({ func, expectedType }) => {
    const functionName = `${func}(foo)`;

    expect(inferTypeForSeries(Series.forFunction(functionName), []))
      .toEqual(FieldTypeMapping.create(functionName, expectedType));
  });

  each`
    func     | fieldType
    ${'avg'} | ${FieldTypes.DATE()}
    ${'min'} | ${FieldTypes.DATE()}
    ${'max'} | ${FieldTypes.DATE()}
    ${'avg'} | ${FieldTypes.LONG()}
    ${'min'} | ${FieldTypes.LONG()}
    ${'max'} | ${FieldTypes.LONG()}
  `.test('returns type of field for "$func(field)"', ({ func, fieldType }) => {
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
    // $FlowFixMe: passing invalid types parameter on purpose.
    expect(inferTypeForSeries(Series.forFunction('avg(foo)'), undefined))
      .toEqual(FieldTypeMapping.create('avg(foo)', FieldType.Unknown));
  });
});
