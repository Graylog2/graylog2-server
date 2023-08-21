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

import type { Token } from 'views/components/searchbar/queryinput/ace-types';
import { existsOperator } from 'views/components/searchbar/completions/FieldNameCompletion';

export const removeFinalColon = (tokenValue: string) => tokenValue.slice(0, -1);

export const isTypeTerm = (token: Token | undefined) => !!(token && token.type === 'term');
export const isTypeString = (token: Token | undefined) => token?.type === 'string';
export const isTypeText = (token: Token | undefined) => token?.type === 'text';
export const isTypeNumber = (token: Token | undefined) => token?.type === 'constant.numeric';

export const isTypeKeyword = (token: Token | undefined) => token?.type === 'keyword';

export const isKeywordOperator = (token: Token | undefined) => token?.type === 'keyword.operator';

export const isExistsOperator = (token: Token | undefined) => isTypeKeyword(token) && token.value === `${existsOperator.name}:`;

export const isCompleteFieldName = (token: Token | undefined) => !!(isTypeKeyword(token) && token?.value.endsWith(':'));

export const isSpace = (currentToken: Token | undefined) => isTypeText(currentToken) && currentToken?.value === ' ';

export const getFieldNameForFieldValueInBrackets = (tokens: Array<Token>, currentTokenIndex: number) => {
  if (!tokens?.length) {
    return null;
  }

  const currentToken = tokens[currentTokenIndex];
  const prevToken = tokens[currentTokenIndex - 1] ?? null;

  if (prevToken?.type === 'keyword' && prevToken?.value.endsWith(':') && currentToken.type === 'paren.lparen') {
    return removeFinalColon(prevToken.value);
  }

  let fieldNameIndex = null;
  let openingBracketsCount = 0;
  let closingBracketsCount = 0;

  for (let i = 0; i < currentTokenIndex; i += 1) {
    if (tokens[i].type === 'keyword' && tokens[i].value.endsWith(':') && tokens[i + 1].type === 'paren.lparen') {
      fieldNameIndex = i;
    }

    if (fieldNameIndex !== null && tokens[i].type === 'paren.lparen') {
      openingBracketsCount += 1;
    }

    if (fieldNameIndex !== null && tokens[i].type === 'paren.rparen') {
      closingBracketsCount += 1;
    }

    if (openingBracketsCount && closingBracketsCount && openingBracketsCount === closingBracketsCount) {
      openingBracketsCount = 0;
      closingBracketsCount = 0;
      fieldNameIndex = null;
    }
  }

  if (fieldNameIndex !== null) {
    return removeFinalColon(tokens[fieldNameIndex].value);
  }

  return null;
};
