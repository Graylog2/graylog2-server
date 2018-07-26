import Pivot from 'enterprise/logic/aggregationbuilder/Pivot';

export const pivotForField = (fieldName) => {
  switch (fieldName) {
    case 'timestamp':
      return new Pivot(fieldName, 'time', { interval: { type: 'auto' } });
    default:
      return new Pivot(fieldName, 'values', { limit: 15 });
  }
};
