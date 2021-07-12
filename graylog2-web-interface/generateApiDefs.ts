const fs = require('fs');

const ts = require('typescript');

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
    return ts.factory.createTypeReferenceNode(mapPrimitiveType(type));
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
  // @ts-ignore
  createProps(definition.properties),
);

// ==== APIs/Operations ==== //
// === Types === //
const createParameter = (({ name, description, required, defaultValue, type }) => ts.factory.createParameterDeclaration(
  undefined,
  undefined,
  undefined,
  name,
  required ? undefined : ts.factory.createToken(ts.SyntaxKind.QuestionToken),
  createTypeFor({ type }),
));

const createResultTypeFor = (typeNode) => ts.factory.createTypeReferenceNode('Promise', [typeNode]);

const createFunction = (parameters, type) => {
  return ts.factory.createFunctionTypeNode(
    undefined,
    parameters.map(createParameter),
    createResultTypeFor(createTypeFor({ type })),
  );
};

const createOperation = (operation, path) => {
  const { nickname, parameters, method, type, path: operationPath } = operation;
  const queryParameters = parameters.filter((parameter) => parameter.paramType === 'query');
  const pathParameters = parameters.filter((parameter) => parameter.paramType === 'path');

  return ts.factory.createPropertySignature(
    [],
    nickname,
    undefined,
    createFunction(parameters, type),
  );
};

const createEndpoint = (endpoint) => {
  const { path, operations } = endpoint;
  const createOperationWithPath = (operation) => createOperation(operation, path);

  return operations.map(createOperationWithPath);
};

const createApiType = (api, name) => {
  const endpoints = api.apis.flatMap(createEndpoint);

  return ts.factory.createInterfaceDeclaration(
    undefined,
    undefined,
    name,
    undefined,
    undefined,
    endpoints,
  );
};

// === Functions === //

const block = ts.factory.createBlock(
  [
    ts.factory.createReturnStatement(
      ts.factory.createCallExpression(
        ts.factory.createIdentifier('fetch'),
        undefined,
        [ts.factory.createIdentifier('foo'), ts.factory.createIdentifier('bar')],
      ),
    ),
  ],
  true,
);

const isNumeric = (type) => ['integer'].includes(type);

const createInitializer = (type, defaultValue) => (isNumeric(type)
  ? ts.factory.createNumericLiteral(defaultValue)
  : ts.factory.createStringLiteral(defaultValue));

const createFunctionParameter = ({ name, description, required, defaultValue, type }) => ts.factory.createParameterDeclaration(
  undefined,
  undefined,
  undefined,
  name,
  required ? undefined : ts.factory.createToken(ts.SyntaxKind.QuestionToken),
  createTypeFor({ type }),
  defaultValue ? createInitializer(type, defaultValue) : undefined,
);

const createRoute = ({ nickname, parameters, method, type, path: operationPath }) => ts.factory.createVariableDeclaration(
  nickname,
  undefined,
  undefined,
  ts.factory.createArrowFunction(
    undefined,
    undefined,
    parameters.map(createFunctionParameter),
    createResultTypeFor(createTypeFor({ type })),
    undefined,
    block,
  ),
);

const createRoutes = (api) => api.operations.map(createRoute);

const createApiObject = (api, name) => {
  const routes = api.apis.flatMap(createRoutes);

  return ts.factory.createExportDeclaration(
    undefined,
    undefined,
    undefined,
    ts.factory.createVariableDeclarationList([
      ts.factory.createVariableDeclaration(
        name,
        undefined,
        undefined,
        ts.factory.createObjectLiteralExpression(
          routes,
          true,
        ),
      ),
    ],
    ts.NodeFlags.Const),
  );
};

// ==== ///
const srcDir = '../target/swagger';
const apiFile = '/streams/alerts.json';

const apiJson = fs.readFileSync(`${srcDir}${apiFile}`).toString();
const api = JSON.parse(apiJson);

const name = 'StreamAlertsApi';
const models = Object.entries(api.models).map(createModel);
const apiDef = createApiType(api, name);
const apiObject = createApiObject(api, name);

const rootNode = ts.factory.createNodeArray([...models, apiDef, apiObject]);

const printer = ts.createPrinter({ newLine: ts.NewLineKind.LineFeed });

const filename = 'source.ts';
const file = ts.createSourceFile(filename, '', ts.ScriptTarget.ESNext, false, ts.ScriptKind.TS);
const result = printer.printList(ts.ListFormat.MultiLine, rootNode, file);

console.log(`===== ${filename} =====`);
console.log(result);
