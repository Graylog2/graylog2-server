// @flow strict
const _convertEmptyString = (value: string) => (value === '' ? undefined : value);

// eslint-disable-next-line import/prefer-default-export
export const createGRN = (type: string, id: string) => `grn::::${type}:${id}`;

export const getValuesFromGRN = (grn: string) => {
  const grnValues = grn.split(':');
  const [resourceNameType, cluster, tenent, scope, type, id] = grnValues.map(_convertEmptyString);

  return { resourceNameType, cluster, tenent, scope, type, id };
};
