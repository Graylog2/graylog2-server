/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
