// @flow strict
import FieldType from 'views/logic/fieldtypes/FieldType';
import Pivot from 'views/logic/aggregationbuilder/Pivot';

import PivotGenerator from './PivotGenerator';

describe('PivotGenerator', () => {
  it('generates time pivot for date fields', () => {
    const result = PivotGenerator('foo', new FieldType('date', [], []));

    expect(result).toEqual(new Pivot('foo', 'time', { interval: { type: 'auto' } }));
  });
  it('generates values pivot for other fields', () => {
    const result = PivotGenerator('foo', new FieldType('keyword', [], []));

    expect(result).toEqual(new Pivot('foo', 'values', { limit: 15 }));
  });
});
