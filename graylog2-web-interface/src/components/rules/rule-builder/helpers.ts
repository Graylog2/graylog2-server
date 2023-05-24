import type { BlockDict, BlockFieldDict, RuleBlockField } from './types';

const extractVariablesFromString = (str : string = '') : Array<{variable: string, variableName: string}> => {
  const variableRegExp = /\$\{(.*?)\}/g;

  const variableMatches = [...str.matchAll(variableRegExp)];

  const variables = variableMatches.map((match) => ({ variable: match[0], variableName: match[1] }));

  return variables;
};

const replaceVariablesWithParams = (params: RuleBlockField | undefined, str: string = '') : string => {
  if (!params) { return str; }

  const variables = extractVariablesFromString(str);

  let newString;

  variables.forEach(({ variableName, variable }) => {
    if (params[variableName]) {
      newString = str.replaceAll(variable, params[variableName] as string);
    }
  });

  return newString;
};

const getDictForFunction = (dict: BlockDict[], functionName: string) : BlockDict | undefined => (
  dict.find((entry) => entry.name === functionName)
);

const getDictForParam = (dict: BlockDict, paramName: string) : BlockFieldDict | undefined => (
  dict.params.find((param) => param.name === paramName)
);

export { extractVariablesFromString, getDictForFunction, getDictForParam, replaceVariablesWithParams };
