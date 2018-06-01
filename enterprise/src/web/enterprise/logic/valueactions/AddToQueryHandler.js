import { trim } from 'lodash';

import { QueriesActions, QueriesStore } from 'enterprise/stores/QueriesStore';

const queryContainsTerm = (query, termInQuestion) => {
  return query.indexOf(termInQuestion) !== -1;
};

const isPhrase = (searchTerm) => {
  return String(searchTerm).indexOf(' ') !== -1;
};

const escape = (searchTerm) => {
  let escapedTerm = String(searchTerm);

  // Replace newlines.
  escapedTerm = escapedTerm.replace(/\r\n/g, ' ');
  escapedTerm = escapedTerm.replace(/\n/g, ' ');
  escapedTerm = escapedTerm.replace(/<br>/g, ' ');

  if (isPhrase(escapedTerm)) {
    escapedTerm = String(escapedTerm).replace(/(\"|\\)/g, '\\$&');
    escapedTerm = `"${escapedTerm}"`;
  } else {
    // Escape all lucene special characters from the source: && || : \ / + - ! ( ) { } [ ] ^ " ~ * ?
    escapedTerm = String(escapedTerm).replace(/(&&|\|\||[:\\\/+\-!(){}[\]^"~*?])/g, '\\$&');
  }

  return escapedTerm;
};

const formatNewQuery = (oldQuery, field, value) => {
  const fieldPredicate = `${field}:${escape(value)}`;

  if (queryContainsTerm(oldQuery, fieldPredicate)) {
    return oldQuery;
  }

  if (trim(oldQuery) === '*' || trim(oldQuery) === '') {
    return fieldPredicate;
  }

  return `${oldQuery} AND ${fieldPredicate}`;
};

export default class AddToQueryHandler {
  constructor() {
    QueriesStore.listen((queries) => { this.queries = queries; });
  }

  handle = (queryId, field, value) => {
    const query = this.queries.get(queryId);
    const oldQuery = query.query.query_string;
    const newQuery = formatNewQuery(oldQuery, field, value);
    QueriesActions.query(queryId, newQuery);
  };
}
