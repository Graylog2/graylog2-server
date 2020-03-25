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
