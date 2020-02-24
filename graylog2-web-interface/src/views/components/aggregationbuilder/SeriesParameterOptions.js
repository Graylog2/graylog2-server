// @flow strict

export const parameterOptionsForType = (type: string): Array<*> => {
  if (type === 'percentile') {
    return [25.0, 50.0, 75.0, 90.0, 95.0, 99.0];
  }
  return [];
};

export const parameterNeededForType = (type: string): boolean => parameterOptionsForType(type).length > 0;

export default { parameterOptionsForType, parameterNeededForType };
