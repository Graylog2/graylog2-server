import * as JSON from 'util/json';

const stringify = (value: any) => {
  switch (typeof value) {
    case 'bigint':
      return value.toString();
    case 'string':
      return value;
    default:
      return JSON.stringify(value);
  }
};

export default stringify;
