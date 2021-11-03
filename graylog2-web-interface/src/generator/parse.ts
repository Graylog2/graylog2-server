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
import fs from 'fs';

import { Model, Api, Route, Operation, Parameter, Type } from 'generator/Api';

const apiFile = '/api.json';

const primitiveTypeMappings = {
  string: 'string',
  String: 'string',
  integer: 'number',
  Integer: 'number',
  long: 'number',
  boolean: 'boolean',
  DateTime: 'string',
  any: 'unknown',
} as const;

const typeMappings = {
  'urn:jsonschema:org:joda:time:DateTime': 'string',
  DateTime: 'string',
  ChunkedOutput: 'unknown',
  ZonedDateTime: 'string',
};
const isPrimitiveType = (type: RawType): type is PrimitiveType => ('type' in type && Object.keys(primitiveTypeMappings).includes(type.type));
const mapPrimitiveType = (type: keyof typeof primitiveTypeMappings) => primitiveTypeMappings[type];
const isEnumType = (type: RawType): type is EnumType => ('enum' in type && type.enum !== undefined);

type Primitives = keyof typeof primitiveTypeMappings;

type PrimitiveType = {
  type: Primitives;
};

type EnumType = {
  type: Primitives;
  enum: Array<string>;
  defaultValue?: string;
}

type ArrayType = {
  type: 'array';
  items: RawType;
}

type ExistingRefType = {
  $ref: string;
}
type LiteralRefType = {
  type: string;
}

type RefType = ExistingRefType | LiteralRefType;

type ObjectType = {
  id: string;
  type: 'object';
  properties: Record<string, RawType>;
  additionalProperties?: RawType;
}

type RawType = PrimitiveType | EnumType | ArrayType | RefType | ObjectType;

const isMappedType = (type: string): type is keyof typeof typeMappings => Object.keys(typeMappings).includes(type);
const mapType = (type: keyof typeof typeMappings) => typeMappings[type];
const isURN = (type: string) => type.split(':').length > 1;

const stripURN = (type) => {
  const splitted = type.split(':');

  return splitted[splitted.length - 1];
};

const isArrayType = (typeDef: RawType): typeDef is ArrayType => ('type' in typeDef && typeDef.type === 'array');
const isObjectType = (typeDef: RawType): typeDef is ObjectType => ('type' in typeDef && typeDef.type === 'object');

const createEnumType = (enumType: EnumType) => {
  const { enum: options, defaultValue } = enumType;
  const mappedPrimitiveType = mapPrimitiveType(enumType.type);

  const result = {
    type: 'enum',
    name: mappedPrimitiveType,
    options,
  } as const;

  return defaultValue !== undefined ? { ...result, defaultValue } : result;
};

function createTypeReference(name: string, optional: boolean = false) {
  return {
    type: 'type_reference',
    name,
    optional,
  } as const;
}

function createArray(items: Type) {
  return {
    type: 'array',
    items,
  } as const;
}

function createTypeLiteralNode(properties: Record<string, Type>, additionalProperties?: Type) {
  return {
    type: 'type_literal',
    properties,
    additionalProperties,
  } as const;
}

const wrapAdditionalProperties = (additionalProperties: RawModel['additional_properties']) => (typeof additionalProperties === 'string' ? ({ type: additionalProperties }) : additionalProperties);

function createType(_typeDefinition: RawType): Type {
  if ('$ref' in _typeDefinition) {
    return createType({ type: _typeDefinition.$ref });
  }

  const { type: rawType } = _typeDefinition;
  const isOptional = rawType && rawType.endsWith('>');

  const cleanType = rawType ? rawType.replace(/>/g, '') : rawType;
  const type = isMappedType(cleanType) ? mapType(cleanType) : cleanType;

  if (type && isURN(type)) {
    return createTypeReference(stripURN(type));
  }

  const typeDefinition = { ..._typeDefinition, type };

  if (isEnumType(typeDefinition)) {
    return createEnumType(typeDefinition);
  }

  if (isPrimitiveType(typeDefinition)) {
    return createTypeReference(mapPrimitiveType(typeDefinition.type));
  }

  if (isArrayType(typeDefinition)) {
    return createArray(createType(wrapAdditionalProperties(typeDefinition.items)));
  }

  if (isObjectType(typeDefinition)) {
    const { id } = typeDefinition;

    if (id && isMappedType(id)) {
      return createType({ type: id });
    }

    const properties = typeDefinition.properties
      ? Object.fromEntries(
        Object.entries(typeDefinition.properties).map(([propName, propType]) => [propName, createType(propType)]),
      )
      : {};

    const additionalProperties = typeDefinition.additionalProperties
      ? createType(typeDefinition.additionalProperties)
      : undefined;

    return createTypeLiteralNode(properties, additionalProperties);
  }

  return createTypeReference(type, isOptional);
}

const bannedModels = [...Object.keys(typeMappings), 'DateTime', 'DateTimeZone', 'Chronology', 'String>', 'LocalDateTime', 'TemporalUnit'];
const isNotBannedModel = ([name]) => !bannedModels.includes(name);

type RawModel = {
  id: string;
  type: string;
  properties: Record<string, RawType>;
  additional_properties?: string | RawType;
}

function createModel(model: RawModel): Model {
  return { ...createType(model), id: model.id };
}

type RawParameter = {
  name: string;
  description: string;
  paramType: 'path' | 'query' | 'body';
  type: string | RawType;
  defaultValue?: string;
  required: boolean;
  enum?: Array<string>,
}

function mergeValues(enumValues: Array<string>, defaultValue: string) {
  if (enumValues === undefined || defaultValue === undefined) {
    return enumValues;
  }

  return [...new Set(enumValues).add(defaultValue)];
}

function createParameter({ name, description, paramType, type, required, defaultValue, enum: enumValues }: RawParameter): Parameter {
  let parameter: Parameter = {
    name,
    description,
    paramType,
    required,
    type: createType(typeof type === 'string' ? { type: type as Primitives, enum: mergeValues(enumValues, defaultValue), defaultValue } : type),
  };

  if (defaultValue !== undefined) {
    parameter = { ...parameter, defaultValue };
  }

  return parameter;
}

type RawContentType = 'application/json';
type RawOperation = {
  summary: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  nickname: string;
  type: string | RawType;
  parameters: Array<RawParameter>;
  path: string;
  produces: Array<RawContentType>;
}

function createOperation({ summary, method, type, parameters, nickname, path, produces }: RawOperation): Operation {
  return {
    method,
    summary,
    nickname,
    type: typeof type === 'string' ? createType({ type }) : createType(type),
    parameters: parameters?.map(createParameter) ?? [],
    path,
    produces,
  };
}

type RawRoute = {
  path: string;
  operations: Array<RawOperation>;
}

function createRoute({ path, operations }: RawRoute): Route {
  return {
    path,
    operations: operations.map(createOperation),
  };
}

function createApiObject(name: string, api: any, models: Record<string, Model>): Api {
  return {
    name,
    description: api.description,
    routes: api.apis.map(createRoute),
    models,
  };
}

type RawApi = {
  models: Record<string, RawModel>;
  apis: Array<RawRoute>;
}

export function parseApi(name: string, api: RawApi) {
  const models = Object.fromEntries(
    Object.entries(api.models)
      .filter(([n]) => isNotBannedModel([n]))
      .map(([n, model]) => [n, createModel(model)]),
  );

  return createApiObject(name, api, models);
}

export default function parse(srcDir: string): Array<Api> {
  const apiSummary = JSON.parse(fs.readFileSync(`${srcDir}/${apiFile}`).toString());

  return apiSummary.apis.map(({ path, name: rawName }) => {
    const name = rawName.replace(/ /g, '');
    const apiJson = fs.readFileSync(`${srcDir}${path}.json`).toString();
    console.log(`Reading ${srcDir}${path}.json`);
    const api = JSON.parse(apiJson) as RawApi;

    return parseApi(name, api);
  });
}
