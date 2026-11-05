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

/**
 * Language which renders English texts with accented letters and additional length. It helps to find texts which are
 * not translated yet and layouts which break with longer texts, as translations are often longer than English.
 */
export const PSEUDO_LANGUAGE = 'en-XA';

const PLAIN = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
const ACCENTED = 'àƀçðéƒĝĥîĵķļɱñöþǫŕšţûṽŵẋýžÀßÇÐÉƑĜĤÎĴĶĻṀÑÖÞǪŔŠŢÛṼŴẊÝŽ';
const ACCENTED_BY_LETTER = Object.fromEntries([...PLAIN].map((letter, index) => [letter, [...ACCENTED][index]]));
const EXPANSION = 0.3;

const pseudoLocalize = (text: string) => {
  const accented = [...text].map((char) => ACCENTED_BY_LETTER[char] ?? char).join('');
  const padding = '~'.repeat(Math.ceil(text.length * EXPANSION));

  return `[${accented}${padding}]`;
};

export default pseudoLocalize;
