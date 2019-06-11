// @flow strict
import Pivot from 'enterprise/logic/aggregationbuilder/Pivot';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';

export default (fieldName: string, type: FieldType) => {
  switch (type.type) {
    case 'date':
      return new Pivot(fieldName, 'time', { interval: { type: 'auto' } });
    default:
      return new Pivot(fieldName, 'values', { limit: 15 });
  }
};
