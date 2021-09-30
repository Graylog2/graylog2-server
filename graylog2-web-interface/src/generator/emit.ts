import * as ts from 'typescript';
import { chunk } from 'lodash';

import { Api, Route, Operation, Parameter, Type, EnumType, TypeLiteral, Model } from 'generator/Api';

const REQUEST_FUNCTION_NAME = '__request__';
const REQUEST_FUNCTION_IMPORT = 'routing/request';
const readonlyModifier = ts.factory.createModifier(ts.SyntaxKind.ReadonlyKeyword);

const emitString = (s: string) => ts.factory.createStringLiteral(s, true);
const quotePropNameIfNeeded = (propName: string) => (propName.match(/^[0-9@]/) ? emitString(propName) : propName);

const typeMappings = {
  'urn:jsonschema:org:joda:time:DateTime': 'string',
  DateTime: 'string',
  ChunkedOutput: 'unknown',
  ZonedDateTime: 'string',
};

const emitNumberOrString = (type) => {
  switch (type) {
    case 'number': return (number: unknown) => ts.factory.createNumericLiteral(number as number);
    default: return (text: unknown) => ts.factory.createStringLiteral(text as string, true);
  }
};

const emitIndexerSignature = (additionalProperties: Type) => (additionalProperties ? [ts.factory.createIndexSignature(
  undefined,
  [readonlyModifier],
  [ts.factory.createParameterDeclaration(
    undefined,
    undefined,
    undefined,
    ts.factory.createIdentifier('_key'),
    undefined,
    ts.factory.createKeywordTypeNode(ts.SyntaxKind.StringKeyword),
    undefined,
  )],
  // eslint-disable-next-line @typescript-eslint/no-use-before-define
  emitType(additionalProperties),
)] : []);

// ===== Models ===== //
const emitProps = (properties: Record<string, Type>) => Object.entries(properties)
  .map(([propName, propDef]) => ts.factory.createPropertySignature(
    [readonlyModifier],
    quotePropNameIfNeeded(propName),
    undefined,
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    emitType(propDef),
  ));

const bannedModels = [...Object.keys(typeMappings), 'DateTime', 'DateTimeZone', 'Chronology', 'String>', 'LocalDateTime', 'TemporalUnit'];
const isNotBannedModel = ([name]: [string, Model]) => !bannedModels.includes(name);

const cleanName = (name: string) => name.replace(/>/g, '');

const emitModel = ([name, definition]: [string, Model]) => (definition.type === 'type_literal'
  ? ts.factory.createInterfaceDeclaration(
    undefined,
    undefined,
    cleanName(name),
    undefined,
    undefined,
    [...emitProps(definition.properties), ...emitIndexerSignature(definition.additionalProperties)],
  )
  : ts.factory.createTypeAliasDeclaration(
    undefined,
    undefined,
    ts.factory.createIdentifier(name),
    undefined,
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    emitType(definition),
  )
);

// ==== APIs/Operations ==== //
// === Types === //
const emitPromiseResultType = (typeNode) => ts.factory.createTypeReferenceNode('Promise', [typeNode]);

// === Functions === //

const extractVariable = (segment) => segment.replace(/[{}]/g, '').split(':')[0];

const emitTemplateString = (path: string) => {
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

const emitBlock = (method: string, path: any, bodyParameter: Parameter, queryParameter: Parameter[], rawProduces: string[]) => {
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
            emitString(method),
            emitTemplateString(path),
            bodyParameter ? ts.factory.createIdentifier(bodyParameter.name) : ts.factory.createNull(),
            queryParameters,
            ts.factory.createObjectLiteralExpression(
              [ts.factory.createPropertyAssignment(
                'Accept',
                ts.factory.createArrayLiteralExpression(produces.map((contentType) => emitString(contentType)), produces.length > 1),
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

const isNumeric = (type: string) => ['integer', 'number'].includes(type);
const isBoolean = (type: string) => ['boolean'].includes(type);

const emitInitializer = (type: Type, defaultValue: string) => {
  const typeName = 'name' in type ? type.name : undefined;

  if (typeName && isNumeric(typeName)) {
    return ts.factory.createNumericLiteral(defaultValue);
  }

  if (typeName && isBoolean(typeName)) {
    switch (defaultValue) {
      case 'true': return ts.factory.createTrue();
      case 'false': return ts.factory.createFalse();
      default: throw new Error(`Invalid boolean value: ${defaultValue}`);
    }
  }

  return emitString(defaultValue);
};

const sortByOptionality = (parameter1: Parameter, parameter2: Parameter) => Number(parameter2.required) - Number(parameter1.required);

const cleanParameterName = (name: string) => name.replace(/\s/g, '');

function emitEnumType({ options, name }: EnumType) {
  const creator = emitNumberOrString(name);
  const types: ts.TypeNode[] = options.map((option) => ts.factory.createLiteralTypeNode(creator(option)));

  return ts.factory.createUnionTypeNode(types);
}

function emitTypeLiteral(type: TypeLiteral) {
  const properties = Object.entries(type.properties ?? [])
    .map(([propName, propType]) => ts.factory.createPropertySignature(
      [readonlyModifier],
      quotePropNameIfNeeded(propName),
      undefined,
      // eslint-disable-next-line @typescript-eslint/no-use-before-define
      emitType(propType),
    ));

  const additionalProperties = emitIndexerSignature(type.additionalProperties);

  return ts.factory.createTypeLiteralNode([...properties, ...additionalProperties]);
}

const assertUnreachable = (ignored: never, message: string): never => {
  throw new Error(`${message}: ${ignored}`);
};

function emitType(type: Type) {
  switch (type.type) {
    case 'array': return ts.factory.createArrayTypeNode(emitType(type.items));
    case 'enum': return emitEnumType(type);
    case 'type_literal': return emitTypeLiteral(type);
    case 'type_reference': return ts.factory.createTypeReferenceNode(type.name);
    default: assertUnreachable(type, 'Unexpected type');
  }

  return undefined;
}

const emitFunctionParameter = ({ name, required, type, defaultValue }: Parameter) => ts.factory.createParameterDeclaration(
  undefined,
  undefined,
  undefined,
  cleanParameterName(name),
  (required || defaultValue) ? undefined : ts.factory.createToken(ts.SyntaxKind.QuestionToken),
  emitType(type),
  defaultValue ? emitInitializer(type, defaultValue as string) : undefined,
);

const firstNonEmpty = (...strings) => strings.find((s) => (s !== undefined && s.trim() !== ''));

const deriveNameFromParameters = (functionName: string, parameters: Parameter[]) => {
  const joinedParameters = parameters.map(({ name }) => cleanParameterName(name)).join('And');

  return `${functionName}By${joinedParameters}`;
};

const bannedFunctionNames = {
  delete: 'remove',
};

const unbanFunctionname = (nickname: string): string => (Object.keys(bannedFunctionNames).includes(nickname) ? bannedFunctionNames[nickname] : nickname);

const emitRoute = ({ nickname, parameters: rawParameters = [], method, type, path: operationPath, summary, produces }: Operation, path: string, assignedNames: Array<string>) => {
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
        undefined,
        [ts.factory.createToken(ts.SyntaxKind.ExportKeyword)],
        undefined,
        ts.factory.createIdentifier(functionName),
        undefined,
        parameters.sort(sortByOptionality).map(emitFunctionParameter),
        emitPromiseResultType(emitType(type)),
        emitBlock(method, firstNonEmpty(operationPath, path) || '/', bodyParameter[0], queryParameters, produces),
      )],
  };
};

type RouteNodes = ReturnType<typeof emitRoute>;

const emitRoutes = (api: Route, alreadyAssigned: Array<string>) => {
  const { operations, path } = api;
  const createRouteWithDefaultPath = (operation: Operation, assignedNames: Array<string>) => emitRoute(operation, path, assignedNames);

  return operations.reduce((prev: Array<RouteNodes>, cur: Operation) => {
    const assigned = [...alreadyAssigned, ...prev.map(({ name }) => name)];
    const newOperation = createRouteWithDefaultPath(cur, assigned);

    return [...prev, newOperation];
  }, []);
};

const emitApiObject = (routes: Array<Route>) => {
  const result = routes.reduce((prev: ReturnType<typeof emitRoutes>, cur: Route) => {
    const assignedNames = prev.map(({ name }) => name);

    const genRoutes = emitRoutes(cur, assignedNames);

    return [...prev, ...genRoutes];
  }, []);

  return result.flatMap(({ nodes }) => nodes);
};

const importDeclaration = ts.factory.createImportDeclaration(
  undefined,
  undefined,
  ts.factory.createImportClause(false, ts.factory.createIdentifier(REQUEST_FUNCTION_NAME), undefined),
  emitString(REQUEST_FUNCTION_IMPORT),
);

const printer = ts.createPrinter({ newLine: ts.NewLineKind.LineFeed });

const cleanIdentifier = (name: string) => name.replace(/\//g, '');

function emitSummary(apis: Array<Api>) {
  const packageIndexFile = ts.createSourceFile('index.ts', '', ts.ScriptTarget.ESNext, false, ts.ScriptKind.TS);
  const reexports = ts.factory.createNodeArray(apis.map(({ name }) => ts.factory.createExportDeclaration(
    undefined,
    undefined,
    false,
    ts.factory.createNamespaceExport(ts.factory.createIdentifier(cleanIdentifier(name))),
    emitString(`./${name}`),
  )));

  return printer.printList(ts.ListFormat.MultiLine, reexports, packageIndexFile);
}

function emitApi({ name, models, routes }: Api) {
  const emittedModels = Object.entries(models)
    .filter(isNotBannedModel)
    .map(emitModel);

  const apiObject = emitApiObject(routes);
  const rootNode = ts.factory.createNodeArray([importDeclaration, ...emittedModels, ...apiObject]);

  const filename = `${name}.ts`;
  const file = ts.createSourceFile(filename, '', ts.ScriptTarget.ESNext, false, ts.ScriptKind.TS);

  return printer.printList(ts.ListFormat.MultiLine, rootNode, file);
}

export default function emit(apis: Array<Api>) {
  const emittedApis = Object.fromEntries(apis.map((api) => [api.name, emitApi(api)]));
  const summary = emitSummary(apis);

  return [emittedApis, summary] as const;
}
