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
// @flow strict
import { trim } from 'lodash';

const isPhrase = (searchTerm: ?string) => {
  return String(searchTerm).indexOf(' ') !== -1;
};

const escape = (searchTerm: ?string) => {
  let escapedTerm = String(searchTerm);

  // Replace newlines.
  escapedTerm = escapedTerm.replace(/\r\n/g, ' ');
  escapedTerm = escapedTerm.replace(/\n/g, ' ');
  escapedTerm = escapedTerm.replace(/<br>/g, ' ');

  if (isPhrase(escapedTerm)) {
    escapedTerm = String(escapedTerm).replace(/(["\\])/g, '\\$&');
    escapedTerm = `"${escapedTerm}"`;
  } else {
    // Escape all lucene special characters from the source: && || : \ / + - ! ( ) { } [ ] ^ " ~ * ?
    escapedTerm = String(escapedTerm).replace(/(&&|\|\||[:\\/+\-!(){}[\]^"~*?])/g, '\\$&');
  }

  return escapedTerm;
};

const addToQuery = (oldQuery: string, newTerm: string, operator: string = 'AND') => {
  if (trim(oldQuery) === '*' || trim(oldQuery) === '') {
    return newTerm;
  }

  if (trim(newTerm) === '*' || trim(newTerm) === '') {
    return oldQuery;
  }

  return `${oldQuery} ${operator} ${newTerm}`;
};

export { isPhrase, escape, addToQuery };
