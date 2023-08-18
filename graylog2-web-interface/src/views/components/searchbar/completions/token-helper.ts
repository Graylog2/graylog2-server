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

import type { Token, Line } from 'views/components/searchbar/queryinput/ace-types';

export const isTerm = (token: Token | undefined) => !!(token && token.type === 'term');
export const isString = (token: Token | undefined) => !!(token && token.type === 'string');

export const isSpecialCharacter = (token: Token | undefined) => !!(token?.value === '.' || token?.type.startsWith('constant'));

export const isFieldValueWithSpecialCharacter = (currentToken: Token | undefined, previousToken: Token | undefined) => {
  if (!isTerm(currentToken) || !previousToken) {
    return false;
  }

  return isSpecialCharacter(previousToken);
};

export const isCompleteFieldName = (token: Token | undefined) => !!(token?.type === 'keyword' && token?.value.endsWith(':'));
export const isProximityCondition = (currentToken: Token | undefined) => currentToken?.type === 'constant.character.proximity';

export const isSpace = (currentToken: Token | undefined) => currentToken?.type === 'text' && currentToken?.value === ' ';
export const isFieldValue = (currentToken: Token | undefined, previousToken: Token | undefined) => isTerm(currentToken) && isCompleteFieldName(previousToken);

export const getFieldNameForFieldValueInBrackets = (tokens: Array<Token>, currentTokenIndex: number) => {
  const currentToken = tokens[currentTokenIndex];
  const prevToken = tokens[currentTokenIndex - 1] ?? null;

  if (prevToken?.type === 'keyword' && prevToken?.value.endsWith(':') && currentToken.type === 'paren.lparen') {
    return prevToken.value.slice(0, -1);
  }

  let fieldNameIndex = null;
  let openingBracketsCount = 0;
  let closingBracketsCount = 0;

  for (let i = 0; i < currentTokenIndex; i++) {
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
    return tokens[fieldNameIndex].value.slice(0, -1);
  }

  return null;
};
