const parseNumber = (num: string | number | undefined) => {
  if (num === undefined) {
    return num as undefined;
  }

  if (typeof num === 'number') {
    return num;
  }

  const parsedNumber = Number.parseInt(num, 10);

  return Number.isNaN(parsedNumber) ? undefined : parsedNumber;
};

export default parseNumber;
