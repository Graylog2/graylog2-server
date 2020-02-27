// @flow strict
import fieldTypeFor from './FieldTypeFor';

import FieldType, { FieldTypes } from './FieldType';
import FieldTypeMapping from './FieldTypeMapping';

describe('FieldTypeFor', () => {
  it('returns `FieldType.Unknown` if field type is not found', () => {
    expect(fieldTypeFor('', [])).toEqual(FieldType.Unknown);
    expect(fieldTypeFor('bar', [FieldTypeMapping.create('foo', FieldTypes.LONG())]))
      .toEqual(FieldType.Unknown);
  });
  it('returns `FieldType.Unknown` if field types are `undefined`', () => {
    // $FlowFixMe: Passing `undefined` types on purpose
    expect(fieldTypeFor('', undefined)).toEqual(FieldType.Unknown);
  });
  it('returns type of field if present', () => {
    expect(fieldTypeFor('foo', [FieldTypeMapping.create('foo', FieldTypes.LONG())]))
      .toEqual(FieldTypes.LONG());
  });
  it('returns inferred type of function if series is passed', () => {
    expect(fieldTypeFor('avg(foo)', [FieldTypeMapping.create('foo', FieldTypes.LONG())]))
      .toEqual(FieldTypes.LONG());
    expect(fieldTypeFor('card(foo)', [FieldTypeMapping.create('foo', FieldTypes.STRING())]))
      .toEqual(FieldTypes.LONG());
  });
  it('returns inferred type (long) for `count()`', () => {
    expect(fieldTypeFor('count()', []))
      .toEqual(FieldTypes.LONG());
  });
});
