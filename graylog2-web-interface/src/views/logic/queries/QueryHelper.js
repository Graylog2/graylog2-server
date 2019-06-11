// @flow strict
import { trim } from 'lodash';

const queryContainsTerm = (query: string, termInQuestion: string) => {
  return query.indexOf(termInQuestion) !== -1;
};

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
  if (queryContainsTerm(oldQuery, newTerm)) {
    return oldQuery;
  }

  if (trim(oldQuery) === '*' || trim(oldQuery) === '') {
    return newTerm;
  }

  return `${oldQuery} ${operator} ${newTerm}`;
};

export { queryContainsTerm, isPhrase, escape, addToQuery };
