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
/* eslint-disable no-console */
import * as fs from 'fs';
import { dirname } from 'path';

import * as ts from 'typescript';
import chunk from 'lodash/chunk';

const REQUEST_FUNCTION_NAME = '__request__';
const REQUEST_FUNCTION_IMPORT = 'routing/request';
const readonlyModifier = ts.factory.createModifier(ts.SyntaxKind.ReadonlyKeyword);

const createString = (s: string) => ts.factory.createStringLiteral(s, true);
const quotePropNameIfNeeded = (propName: string) => (propName.match(/^[0-9@]/) ? createString(propName) : propName);

// ===== Types ===== //
const primitiveTypeMappings = {
  string: 'string',
  integer: 'number',
  Integer: 'number',
  long: 'number',
  boolean: 'boolean',
  DateTime: 'string',
  String: 'string',
  any: 'unknown',
};

type Type = string;
type AdditionalProperties = string | PropDef;
type Properties = { [key: string]: PropDef };

type PropDef = {
  type: Type,
  id?: Type,
  $ref?: string,
  properties?: Properties,
  additionalProperties?: AdditionalProperties,
  enum?: Array<string>,
  defaultValue?: string,
  items?: PropDef,
}
const isArrayType = (type: Type) => (type === 'array');
const isObjectType = (type: Type) => (type === 'object');
const isPrimitiveType = (type: Type) => Object.keys(primitiveTypeMappings).includes(type);
const mapPrimitiveType = (type: Type) => primitiveTypeMappings[type];

const typeMappings = {
  'urn:jsonschema:org:joda:time:DateTime': 'string',
  DateTime: 'string',
  ChunkedOutput: 'unknown',
  ZonedDateTime: 'string',
};

const isURN = (type: Type) => type.split(':').length > 1;

const stripURN = (type: Type) => {
  const splitted = type.split(':');

  return splitted[splitted.length - 1];
};

const isMappedType = (type: Type) => Object.keys(typeMappings).includes(type);
const mapType = (type: Type) => typeMappings[type];

const isEnumType = (propDef: PropDef) => propDef.enum !== undefined;

const creatorForType = (type: Type) => {
  switch (type) {
    case 'number': return (number: string | number) => ts.factory.createLiteralTypeNode(
      ts.factory.createNumericLiteral(number),
    );
    default: return (text: string) => ts.factory.createLiteralTypeNode(
      ts.factory.createStringLiteral(text, true),
    );
  }
};

const createEnumType = ({ type, enum: enumOptions, defaultValue }: PropDef) => {
  const mappedPrimitiveType = mapPrimitiveType(type);
  const creator = creatorForType(mappedPrimitiveType);
  const options = defaultValue ? [...new Set(enumOptions).add(defaultValue)] : enumOptions;
  const types = options.map((option) => creator(option));

  return ts.factory.createUnionTypeNode(types as ts.TypeNode[]);
};

const wrapAdditionalProperties = (additionalProperties: AdditionalProperties) => (typeof additionalProperties === 'string' ? ({ type: additionalProperties } as PropDef) : additionalProperties);

const createIndexerSignature = (additionalProperties: AdditionalProperties) => (additionalProperties ? [ts.factory.createIndexSignature(
  [readonlyModifier],
  [ts.factory.createParameterDeclaration(
    undefined,
    undefined,
    ts.factory.createIdentifier('_key'),
    undefined,
    ts.factory.createKeywordTypeNode(ts.SyntaxKind.StringKeyword),
  )],
  // eslint-disable-next-line @typescript-eslint/no-use-before-define
  createTypeFor(wrapAdditionalProperties(additionalProperties)),
)] : []);

const createTypeFor = (propDef: PropDef) => {
  const { type: rawType, ...rest } = propDef;
  const isOptional = rawType && rawType.endsWith('>');

  const cleanType = rawType ? rawType.replace(/>/g, '') : rawType;
  const type = isMappedType(cleanType) ? mapType(cleanType) : cleanType;

  if (type && isURN(type)) {
    return ts.factory.createTypeReferenceNode(stripURN(type));
  }

  if (propDef.$ref) {
    return createTypeFor({ type: propDef.$ref });
  }

  if (isPrimitiveType(type)) {
    return isEnumType(propDef) ? createEnumType(propDef) : ts.factory.createTypeReferenceNode(mapPrimitiveType(type));
  }

  if (isArrayType(type)) {
    return ts.factory.createArrayTypeNode(createTypeFor(rest.items));
  }

  if (isObjectType(type)) {
    const { id } = propDef;

    if (id && isMappedType(id)) {
      return createTypeFor({ type: id });
    }

    const properties = propDef.properties
      ? Object.entries(propDef.properties).map(([propName, propType]) => ts.factory.createPropertySignature(
        [readonlyModifier],
        quotePropNameIfNeeded(propName),
        undefined,
        createTypeFor(propType),
      ))
      : [];

    const additionalProperties = createIndexerSignature(propDef.additionalProperties);

    return ts.factory.createTypeLiteralNode([...properties, ...additionalProperties]);
  }

  const typeReferenceNode = ts.factory.createTypeReferenceNode(type);

  return isOptional
    ? ts.factory.createUnionTypeNode([typeReferenceNode, ts.factory.createKeywordTypeNode(ts.SyntaxKind.UndefinedKeyword)])
    : typeReferenceNode;
};

// ===== Models ===== //
const createProps = (properties: Properties) => Object.entries(properties)
  .map(([propName, propDef]) => ts.factory.createPropertySignature(
    [readonlyModifier],
    quotePropNameIfNeeded(propName),
    undefined,
    createTypeFor(propDef),
  ));

const bannedModels = [...Object.keys(typeMappings), 'DateTime', 'DateTimeZone', 'Chronology', 'String>', 'LocalDateTime', 'TemporalUnit'];
const isNotBannedModel = ([name]) => !bannedModels.includes(name);

const cleanName = (name) => name.replace(/>/g, '');

const createModel = ([name, definition]) => (definition.type === 'object'
  ? ts.factory.createInterfaceDeclaration(
    undefined,
    cleanName(name),
    undefined,
    undefined,
    [...createProps(definition.properties), ...createIndexerSignature(definition.additional_properties)],
  )
  : ts.factory.createTypeAliasDeclaration(
    undefined,
    ts.factory.createIdentifier(name),
    undefined,
    createTypeFor(definition),
  )
);

// ==== APIs/Operations ==== //
// === Types === //
const createResultTypeFor = (typeNode: ts.TypeNode) => ts.factory.createTypeReferenceNode('Promise', [typeNode]);

// === Functions === //

const extractVariable = (segment: string) => segment.replace(/[{}]/g, '').split(':')[0];

const createTemplateString = (path: string) => {
  const segments = path.split(/({.+?})/);

  if (segments.length === 1) {
    return ts.factory.createStringLiteral(path, true);
  }

  const headSegment = segments[0];
  const headIsSpan = headSegment.startsWith('{');
  const head = ts.factory.createTemplateHead(headIsSpan ? '' : headSegment);

  const spanSegments = headIsSpan ? segments : segments.slice(1);

  const chunks = chunk(spanSegments, 2);
  const spans = chunks.flatMap(([variable, text], index) => {
    const isLastChunk = index === chunks.length - 1;
    const identifier = ts.factory.createIdentifier(extractVariable(variable));
    const literalText = text as string || '';
    const literal = isLastChunk ? ts.factory.createTemplateTail(literalText) : ts.factory.createTemplateMiddle(literalText);

    return ts.factory.createTemplateSpan(identifier, literal);
  });

  return ts.factory.createTemplateExpression(
    head,
    spans,
  );
};

const createBlock = (method, path, bodyParameter, queryParameter, rawProduces) => {
  const produces = rawProduces || [];
  const queryParameters = ts.factory.createObjectLiteralExpression(
    queryParameter.map((q) => ts.factory.createPropertyAssignment(
      q.name,
      ts.factory.createIdentifier(q.name),
    )),
  );

  return ts.factory.createBlock(
    [
      ts.factory.createReturnStatement(
        ts.factory.createCallExpression(
          ts.factory.createIdentifier(REQUEST_FUNCTION_NAME),
          undefined,
          [
            createString(method),
            createTemplateString(path),
            bodyParameter ? ts.factory.createIdentifier(bodyParameter.name) : ts.factory.createNull(),
            queryParameters,
            ts.factory.createObjectLiteralExpression(
              [ts.factory.createPropertyAssignment(
                'Accept',
                ts.factory.createArrayLiteralExpression(produces.map((contentType) => createString(contentType)), produces.length > 1),
              )],
              true,
            ),
          ],
        ),
      ),
    ],
    true,
  );
};

const isNumeric = (type: Type) => ['integer', 'number'].includes(type);
const isBoolean = (type: Type) => ['boolean'].includes(type);

const createInitializer = (type: Type, defaultValue) => {
  if (isNumeric(type)) {
    return ts.factory.createNumericLiteral(defaultValue);
  }

  if (isBoolean(type)) {
    switch (defaultValue) {
      case 'true': return ts.factory.createTrue();
      case 'false': return ts.factory.createFalse();
      default: throw new Error(`Invalid boolean value: ${defaultValue}`);
    }
  }

  return createString(defaultValue);
};

const sortByOptionality = (parameter1, parameter2) => parameter2.required - parameter1.required;

const cleanParameterName = (name: string) => name.replace(/\s/g, '');

const createFunctionParameter = ({ name, required, defaultValue, type, enum: allowableValues }) => {
  const mappedType = isPrimitiveType(type) ? mapPrimitiveType(type) : type;

  return ts.factory.createParameterDeclaration(
    undefined,
    undefined,
    cleanParameterName(name),
    (required || defaultValue) ? undefined : ts.factory.createToken(ts.SyntaxKind.QuestionToken),
    createTypeFor({ type: mappedType, enum: allowableValues, defaultValue }),
    defaultValue ? createInitializer(mappedType, defaultValue) : undefined,
  );
};

const firstNonEmpty = (...strings: Array<string>) => strings.find((s) => (s !== undefined && s.trim() !== ''));

const deriveNameFromParameters = (functionName: string, parameters) => {
  const joinedParameters = parameters.map(({ name }) => cleanParameterName(name)).join('And');

  return `${functionName}By${joinedParameters}`;
};

const bannedFunctionNames = {
  delete: 'remove',
};

const unbanFunctionname = (nickname: string) => (Object.keys(bannedFunctionNames).includes(nickname) ? bannedFunctionNames[nickname] : nickname);

const createRoute = ({ nickname, parameters: rawParameters = [], method, type, path: operationPath, summary, produces }, path, assignedNames) => {
  const parameters = rawParameters.map((parameter) => ({ ...parameter, name: cleanParameterName(parameter.name) }));
  const queryParameters = parameters.filter((parameter) => parameter.paramType === 'query');
  const bodyParameter = parameters.filter((parameter) => parameter.paramType === 'body');

  const jsDoc = ts.factory.createJSDocComment(summary,
    ts.factory.createNodeArray(
      parameters.filter((p) => p.description)
        .map((p) => ts.factory.createJSDocParameterTag(undefined, ts.factory.createIdentifier(p.name), undefined, undefined, undefined, p.description)),
    ));

  const unbannedNickname = unbanFunctionname(nickname);

  const functionName = assignedNames.includes(unbannedNickname)
    ? deriveNameFromParameters(unbannedNickname, parameters)
    : unbannedNickname;

  return {
    name: functionName,
    nodes: [
      jsDoc,
      ts.factory.createFunctionDeclaration(
        [ts.factory.createToken(ts.SyntaxKind.ExportKeyword)],
        undefined,
        ts.factory.createIdentifier(functionName),
        undefined,
        parameters.sort(sortByOptionality).map(createFunctionParameter),
        createResultTypeFor(createTypeFor({ type })),
        createBlock(method, firstNonEmpty(operationPath, path) || '/', bodyParameter[0], queryParameters, produces),
      )],
  };
};

const createRoutes = (api, alreadyAssigned) => {
  const { operations, path } = api;
  const createRouteWithDefaultPath = (operation, assignedNames) => createRoute(operation, path, assignedNames);

  return operations.reduce((prev, cur) => {
    const assigned = [...alreadyAssigned, ...prev.map(({ name }) => name)];
    const newOperation = createRouteWithDefaultPath(cur, assigned);

    return [...prev, newOperation];
  }, []);
};

const createApiObject = (api) => {
  const result = api.apis.reduce((prev, cur) => {
    const assignedNames = prev.map(({ name }) => name);

    const genRoutes = createRoutes(cur, assignedNames);

    return [...prev, ...genRoutes];
  }, []);

  return result.flatMap(({ nodes }) => nodes);
};

const importDeclaration = ts.factory.createImportDeclaration(
  undefined,
  ts.factory.createImportClause(false, ts.factory.createIdentifier(REQUEST_FUNCTION_NAME), undefined),
  createString(REQUEST_FUNCTION_IMPORT),
);

// ==== ///

const [srcDir, dstDir] = process.argv.slice(2);

const apiFile = '/api.json';

const apiSummary = JSON.parse(fs.readFileSync(`${srcDir}/${apiFile}`).toString());

const printer = ts.createPrinter({ newLine: ts.NewLineKind.LineFeed });

fs.mkdirSync(dstDir, { recursive: true });

const apisSet = new Set();

apiSummary.apis.forEach(({ path, name: rawName }) => {
  const name = rawName.replace(/ /g, '');
  const apiJson = fs.readFileSync(`${srcDir}${path}.json`).toString();
  const api = JSON.parse(apiJson);

  const models = Object.entries(api.models)
    .filter(([n]) => isNotBannedModel([n]))
    .map(createModel);
  const apiObject = createApiObject(api);

  const rootNode = ts.factory.createNodeArray([importDeclaration, ...models, ...apiObject]);

  const filename = `${name}.ts`;
  const file = ts.createSourceFile(filename, '', ts.ScriptTarget.ESNext, false, ts.ScriptKind.TS);
  const result = printer.printList(ts.ListFormat.MultiLine, rootNode, file);

  const fullFilename = `${dstDir}/${filename}`;
  console.log(`Generating ${fullFilename} ...`);

  const directory = dirname(fullFilename);
  fs.mkdirSync(directory, { recursive: true });

  fs.writeFileSync(fullFilename, result);
  apisSet.add(name);
});

const cleanIdentifier = (name) => name.replace(/\//g, '');

const apis = [...apisSet];
const packageIndexFile = ts.createSourceFile('index.ts', '', ts.ScriptTarget.ESNext, false, ts.ScriptKind.TS);
const reexports = ts.factory.createNodeArray(apis.map((name) => ts.factory.createExportDeclaration(
  undefined,
  false,
  ts.factory.createNamespaceExport(ts.factory.createIdentifier(cleanIdentifier(name))),
  createString(`./${name}`),
)));

const packageIndex = printer.printList(ts.ListFormat.MultiLine, reexports, packageIndexFile);
const fullPackageIndexPath = `${dstDir}/index.ts`;
fs.writeFileSync(fullPackageIndexPath, packageIndex);

console.log('done.');
