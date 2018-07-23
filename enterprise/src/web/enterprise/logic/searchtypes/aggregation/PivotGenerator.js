import Pivot from 'enterprise/logic/aggregationbuilder/Pivot';

export const pivotForField = (fieldName) => {
  switch (fieldName) {
    case 'timestamp':
      return new Pivot(fieldName, 'time', { interval: { value: 1, unit: 'minutes' } });
    default:
      return new Pivot(fieldName, 'values', { limit: 15 });
  }
};
