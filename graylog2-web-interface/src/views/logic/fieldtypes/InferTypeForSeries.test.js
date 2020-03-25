// @flow strict

// $FlowFixMe: imports from core need to be fixed in flow
import each from 'jest-each';

import inferTypeForSeries from './InferTypeForSeries';
import Series from '../aggregationbuilder/Series';
import FieldTypeMapping from './FieldTypeMapping';
import FieldType, { FieldTypes } from './FieldType';

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
