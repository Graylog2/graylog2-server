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
const isEnumType = (type: PrimitiveType): type is PrimitiveEnumType<typeof type['type']> => ('enum' in type);

type Primitives = keyof typeof primitiveTypeMappings;
type MapPrimitiveType<R extends Primitives> = R extends 'string'
  ? string
  : R extends 'String'
    ? string
    : R extends 'integer'
      ? number
      : R extends 'Integer'
        ? number
        : R extends 'long'
          ? number
          : R extends 'boolean'
            ? boolean
            : R extends 'DateTime'
              ? string
              : unknown;

type PrimitiveType = {
  type: Primitives;
};

type PrimitiveEnumType<R extends Primitives> = {
  type: R;
  enum: Array<MapPrimitiveType<R>>;
  defaultValue?: MapPrimitiveType<R>;
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

type RawType = PrimitiveType | ArrayType | RefType | ObjectType;

const isMappedType = (type: string): type is keyof typeof typeMappings => Object.keys(typeMappings).includes(type);
const mapType = (type: keyof typeof typeMappings) => typeMappings[type];
const isURN = (type: string) => type.split(':').length > 1;

const stripURN = (type) => {
  const splitted = type.split(':');

  return splitted[splitted.length - 1];
};

const isArrayType = (typeDef: RawType): typeDef is ArrayType => ('type' in typeDef && typeDef.type === 'array');
const isObjectType = (typeDef: RawType): typeDef is ObjectType => ('type' in typeDef && typeDef.type === 'object');

const createEnumType = <T extends Primitives>({ type, enum: enumOptions, defaultValue }: PrimitiveEnumType<T>) => {
  const mappedPrimitiveType = mapPrimitiveType(type);
  const options = defaultValue ? [...new Set(enumOptions).add(defaultValue)] : enumOptions;

  return {
    type: 'enum',
    name: mappedPrimitiveType,
    options,
    defaultValue,
  } as const;
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

  if (isPrimitiveType(typeDefinition)) {
    return isEnumType(typeDefinition)
      ? createEnumType(typeDefinition)
      : createTypeReference(mapPrimitiveType(typeDefinition.type));
  }

  if (isArrayType(typeDefinition)) {
    return createArray(createType(typeDefinition.items));
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
  type: 'object';
  properties: Record<string, RawType>;
  additional_properties?: string | RawType;
}

const wrapAdditionalProperties = (additionalProperties: RawModel['additional_properties']) => (typeof additionalProperties === 'string' ? ({ type: additionalProperties }) : additionalProperties);

function createModel({ id, properties, additional_properties }: RawModel): Model {
  return {
    id,
    type: 'type_literal',
    properties: Object.fromEntries(
      Object.entries(properties)
        .map(([name, rawType]) => [name, createType(rawType)]),
    ),
    additionalProperties: additional_properties ? createType(wrapAdditionalProperties(additional_properties)) : undefined,
  };
}

type RawParameter = {
  name: string;
  description: string;
  paramType: 'path' | 'query' | 'body';
  type: string | RawType;
  required: boolean;
}

function createParameter({ name, description, paramType, type, required }: RawParameter): Parameter {
  return {
    name,
    description,
    paramType,
    required,
    type: createType(typeof type === 'string' ? { type } : type),
  };
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

export default function parse(srcDir: string): Array<Api> {
  const apiSummary = JSON.parse(fs.readFileSync(`${srcDir}/${apiFile}`).toString());

  return apiSummary.apis.map(({ path, name: rawName }) => {
    const name = rawName.replace(/ /g, '');
    const apiJson = fs.readFileSync(`${srcDir}${path}.json`).toString();
    console.log(`Reading ${srcDir}${path}.json`);
    const api = JSON.parse(apiJson) as RawApi;

    const models = Object.fromEntries(
      Object.entries(api.models)
        .filter(([n]) => isNotBannedModel([n]))
        .map(([n, model]) => [n, createModel(model)]),
    );

    return createApiObject(name, api, models);
  });
}
