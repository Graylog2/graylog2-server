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
import type { BlockDict, BlockFieldDict } from './types';

const getDictForFunction = (dict: BlockDict[], functionName: string) : BlockDict | undefined => (
  dict.find((entry) => entry.name === functionName)
);

const getDictForParam = (dict: BlockDict, paramName: string) : BlockFieldDict | undefined => (
  dict.params.find((param) => param.name === paramName)
);

const getActionOutputVariableName = (order : number) : string => {
  if (order === 0) return '';

  return `output_actions_${order}`;
};

const paramValueExists = (paramValue: string | number | boolean) : boolean => (
  typeof paramValue !== 'undefined' && paramValue !== null);

const paramValueIsVariable = (paramValue: string | number | boolean) : boolean => (
  typeof paramValue === 'string' && paramValue.startsWith('$'));

export { getActionOutputVariableName, getDictForFunction, getDictForParam, paramValueExists, paramValueIsVariable };
