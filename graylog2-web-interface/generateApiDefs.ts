const fs = require('fs');
const { dirname } = require('path');

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
const createResultTypeFor = (typeNode) => ts.factory.createTypeReferenceNode('Promise', [typeNode]);

// === Functions === //

const transformTemplate = (path) => path.replace(/{/g, '${');

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
            ts.factory.createStringLiteral(method),
            ts.factory.createStringLiteral(transformTemplate(path)),
            bodyParameter ? ts.factory.createIdentifier('body') : ts.factory.createNull(),
            queryParameters,
            ts.factory.createObjectLiteralExpression(
              [ts.factory.createPropertyAssignment(
                'accepts',
                ts.factory.createArrayLiteralExpression(produces.map((contentType) => ts.factory.createStringLiteral(contentType)), produces.length > 1),
              )],
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
  : ts.factory.createStringLiteral(defaultValue));

const createFunctionParameter = ({ name, required, defaultValue, type, paramType }) => ts.factory.createParameterDeclaration(
  undefined,
  undefined,
  undefined,
  paramType === 'body' ? 'body' : name,
  required ? undefined : ts.factory.createToken(ts.SyntaxKind.QuestionToken),
  createTypeFor({ type }),
  defaultValue ? createInitializer(type, defaultValue) : undefined,
);

const firstNonEmpty = (...strings) => strings.find((s) => (s !== undefined && s.trim() !== ''));

const createRoute = ({ nickname, parameters = [], method, type, path: operationPath, summary, produces }, path) => {
  const queryParameters = parameters.filter((parameter) => parameter.paramType === 'query');
  const bodyParameter = parameters.filter((parameter) => parameter.paramType === 'body');

  const jsDoc = ts.factory.createJSDocComment(summary,
    ts.factory.createNodeArray(
      parameters.filter((p) => p.description)
        .map((p) => ts.factory.createJSDocParameterTag(undefined, ts.factory.createIdentifier(p.name), undefined, undefined, undefined, p.description)),
    ));

  return [
    jsDoc,
    ts.factory.createVariableDeclaration(
      nickname,
      undefined,
      undefined,
      ts.factory.createArrowFunction(
        undefined,
        undefined,
        parameters.map(createFunctionParameter),
        createResultTypeFor(createTypeFor({ type })),
        undefined,
        createBlock(method, firstNonEmpty(operationPath, path) || '/', bodyParameter[0], queryParameters, produces),
      ),
    )];
};

const createRoutes = (api) => {
  const { operations, path } = api;
  const createRouteWithDefaultPath = (operation) => createRoute(operation, path);

  return operations.flatMap(createRouteWithDefaultPath);
};

const createApiObject = (api, name) => {
  const routes = api.apis.flatMap(createRoutes);
  const cleanName = name.replace(/\//g, '');

  return ts.factory.createExportDeclaration(
    undefined,
    undefined,
    undefined,
    ts.factory.createVariableDeclarationList([
      ts.factory.createVariableDeclaration(
        cleanName,
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

const importDeclaration = ts.factory.createImportDeclaration(
  undefined,
  undefined,
  ts.factory.createImportClause(false, ts.factory.createIdentifier('request')),
  ts.factory.createStringLiteral('routing/request'),
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
  const apiObject = createApiObject(api, name);

  const rootNode = ts.factory.createNodeArray([importDeclaration, ...models, apiObject]);

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
