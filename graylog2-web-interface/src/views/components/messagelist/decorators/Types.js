// @flow strict
export type Decorator = {
  id?: string,
  order: number,
  type: string,
  stream: ?string,
};

// Not properly typed yet, but not needed for the current scope.
export type RequestedConfiguration = {};

export type DecoratorType = {
  type: string,
  name: string,
  human_name: string,
  requested_configuration: RequestedConfiguration,
  link_to_docs: string,
};
