// @flow strict

const isNumeric = (str: any): %checks => {
  if (typeof str === 'number') return true;
  if (typeof str !== 'string') return false;

  if (str.trim() === '') return false;

  return !Number.isNaN(Number(str));
};

export default isNumeric;
