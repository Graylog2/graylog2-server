/* eslint-disable no-console */
// import * as ts from 'typescript';

const fs = require('fs');
const { dirname } = require('path');

const { chunk } = require('lodash');
const ts = require('typescript');

const createString = (s) => ts.factory.createStringLiteral(s, true);

// ===== Types ===== //
const primitiveTypeMappings = {
  string: 'string',
  integer: 'number',
  boolean: 'boolean',
  DateTime: 'Date',
  object: '{}',
};

const isArrayType = (type) => (type === 'array');
const isPrimitiveType = (type) => Object.keys(primitiveTypeMappings).includes(type);
const mapPrimitiveType = (type) => primitiveTypeMappings[type];

const typeMappings = {
  'urn:jsonschema:org:joda:time:DateTime': 'Date',
};

const isURN = (type) => type.split(':').length > 1;

const stripURN = (type) => {
  const splitted = type.split(':');

  return splitted[splitted.length - 1];
};

const isMappedType = (type) => Object.keys(typeMappings).includes(type);
const mapType = (type) => typeMappings[type];

const isEnumType = (propDef) => propDef.enum !== undefined;

const creatorForType = (type) => {
  switch (type) {
    case 'number': return (number) => ts.factory.createNumericLiteral(number);
    default: return (text) => ts.factory.createStringLiteral(text, true);
  }
};

const createEnumType = ({ type, enum: enumOptions }) => {
  const mappedPrimitiveType = mapPrimitiveType(type);
  const creator = creatorForType(mappedPrimitiveType);
  const types = enumOptions.map((option) => creator(option));

  return ts.factory.createUnionTypeNode(types);
};

const createTypeFor = (propDef) => {
  const { type, ...rest } = propDef;

  if (type && isURN(type)) {
    return ts.factory.createTypeReferenceNode(stripURN(type));
  }

  if (isArrayType(type)) {
    return ts.factory.createArrayTypeNode(createTypeFor(rest.items));
  }

  if (isMappedType(type)) {
    return ts.factory.createTypeReferenceNode(mapType(type));
  }

  if (propDef.$ref) {
    return createTypeFor({ type: propDef.$ref });
  }

  if (isPrimitiveType(type)) {
    return isEnumType(propDef) ? createEnumType(propDef) : ts.factory.createTypeReferenceNode(mapPrimitiveType(type));
  }

  return ts.factory.createTypeReferenceNode(type);
};

const readonlyModifier = ts.factory.createModifier(ts.SyntaxKind.ReadonlyKeyword);

// ===== Models ===== //
const createProps = (properties) => Object.entries(properties)
  .map(([propName, propDef]) => ts.factory.createPropertySignature(
    [readonlyModifier],
    propName,
    undefined,
    createTypeFor(propDef),
  ));

const createModel = ([name, definition]) => ts.factory.createInterfaceDeclaration(
  undefined,
  undefined,
  name,
  undefined,
  undefined,
  createProps(definition.properties),
);

// ==== APIs/Operations ==== //
// === Types === //
const createResultTypeFor = (typeNode) => ts.factory.createTypeReferenceNode('Promise', [typeNode]);

// === Functions === //

const extractVariable = (segment) => segment.replace(/[{}]/g, '');

const createTemplateString = (path) => {
  const segments = path.split(/({.+?})/);

  if (segments.length === 1) {
    return ts.factory.createStringLiteral(path);
  }

  const headSegment = segments[0];
  const headIsSpan = headSegment.startsWith('{');
  const head = ts.factory.createTemplateHead(headIsSpan ? '' : headSegment);

  const spanSegments = headIsSpan ? segments : segments.slice(1);

  const chunks = chunk(spanSegments, 2);
  const spans = chunks.flatMap(([variable, text], index) => {
    const isLastChunk = index === chunks.length - 1;
    const identifier = ts.factory.createIdentifier(extractVariable(variable));
    const literalText = text || '';
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
          ts.factory.createIdentifier('request'),
          undefined,
          [
            createString(method),
            createTemplateString(path),
            bodyParameter ? ts.factory.createIdentifier('body') : ts.factory.createNull(),
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

const isNumeric = (type) => ['integer'].includes(type);

const createInitializer = (type, defaultValue) => (isNumeric(type)
  ? ts.factory.createNumericLiteral(defaultValue)
  : createString(defaultValue));

const sortByOptionality = (parameter1, parameter2) => parameter2.required - parameter1.required;

const createFunctionParameter = ({ name, required, defaultValue, type, paramType }) => ts.factory.createParameterDeclaration(
  undefined,
  undefined,
  undefined,
  paramType === 'body' ? 'body' : name,
  (required || defaultValue) ? undefined : ts.factory.createToken(ts.SyntaxKind.QuestionToken),
  createTypeFor({ type }),
  defaultValue ? createInitializer(type, defaultValue) : undefined,
);

const firstNonEmpty = (...strings) => strings.find((s) => (s !== undefined && s.trim() !== ''));

const deriveNameFromParameters = (functionName, parameters) => {
  const joinedParameters = parameters.map(({ name }) => name).join('And');

  return `${functionName}By${joinedParameters}`;
};

const bannedFunctionNames = {
  delete: 'remove',
};

const unbanFunctionname = (nickname) => (Object.keys(bannedFunctionNames).includes(nickname) ? bannedFunctionNames[nickname] : nickname);

const createRoute = ({ nickname, parameters = [], method, type, path: operationPath, summary, produces }, path, assignedNames) => {
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
        undefined,
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
  undefined,
  ts.factory.createImportClause(false, ts.factory.createIdentifier('request'), undefined),
  createString('routing/request'),
);

// ==== ///

const [srcDir, dstDir] = process.argv.slice(2);

const apiFile = '/api.json';

const apiSummary = JSON.parse(fs.readFileSync(`${srcDir}/${apiFile}`).toString());

const printer = ts.createPrinter({ newLine: ts.NewLineKind.LineFeed });

fs.mkdirSync(dstDir, { recursive: true });

apiSummary.apis.forEach(({ path, name: rawName }) => {
  const name = rawName.replace(/ /g, '');
  const apiJson = fs.readFileSync(`${srcDir}${path}.json`).toString();
  const api = JSON.parse(apiJson);

  const models = Object.entries(api.models).map(createModel);
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
});

console.log('done.');
