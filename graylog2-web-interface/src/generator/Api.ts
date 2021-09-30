interface ArrayType {
  type: 'array';
  items: Type;
}
export interface EnumType {
  type: 'enum';
  name: 'string' | 'number' | 'boolean' | 'unknown';
  options: Array<unknown>;
  defaultValue?: unknown;
}
export interface TypeLiteral {
  type: 'type_literal';
  properties: Record<string, Type>;
  additionalProperties?: Type;
}
interface TypeReference {
  type: 'type_reference';
  name: string;
  optional: boolean;
}
export type Type = ArrayType | TypeLiteral | TypeReference | EnumType;

export type Model = Type & {
  id: string;
}

export interface Parameter {
  name: string;
  description: string;
  required: boolean;
  paramType: 'path' | 'query' | 'body';
  type: Type;
  defaultValue?: string;
}

type ContentType = 'application/json';

export interface Operation {
  summary: string;
  nickname: string;
  path: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  parameters: Array<Parameter>;
  type: Type;
  produces: Array<ContentType>;
}

export interface Route {
  path: string;
  operations: Array<Operation>;
}

export interface Api {
  name: string;
  description: string;
  models: Record<string, Model>;
  routes: Array<Route>;
}
