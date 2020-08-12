// @flow strict

// eslint-disable-next-line import/prefer-default-export
export const createGRN = (id: string, type: string) => `grn::::${type}:${id}`;

export const getIdFromGRN = (grn: string, type: string) => {
  const grnStart = `grn::::${type}:`;

  return grn.replace(grnStart, '');
};
