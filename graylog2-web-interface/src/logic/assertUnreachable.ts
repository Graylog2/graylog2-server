const serialize = (o: any) => JSON.stringify(o, null, 2);

const assertUnreachable = (ignored: never, message: string): never => {
  throw new Error(`${message}: ${serialize(ignored)}`);
};

export default assertUnreachable;
