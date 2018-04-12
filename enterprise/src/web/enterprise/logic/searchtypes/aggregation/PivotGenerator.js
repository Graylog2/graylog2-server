export const pivotForField = (fieldName) => {
  switch (fieldName) {
    case 'timestamp':
      return { field: fieldName, type: 'time', config: { interval: { value: 1, unit: 'minutes' } } };
    default:
      return { field: fieldName, type: 'values', config: {} };
  }
};
