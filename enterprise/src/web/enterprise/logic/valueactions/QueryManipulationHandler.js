// @flow strict
import * as Immutable from 'immutable';
import { trim } from 'lodash';
import { QueriesStore } from 'enterprise/stores/QueriesStore';
import Query from '../queries/Query';

export default class QueryManipulationHandler {
  queries: Immutable.Set<Query>;
  constructor() {
    QueriesStore.listen((queries) => { this.queries = queries; });
  }

  queryContainsTerm = (query: string, termInQuestion: string) => {
    return query.indexOf(termInQuestion) !== -1;
  };

  isPhrase = (searchTerm: ?string) => {
    return String(searchTerm).indexOf(' ') !== -1;
  };

  escape = (searchTerm: ?string) => {
    let escapedTerm = String(searchTerm);

    // Replace newlines.
    escapedTerm = escapedTerm.replace(/\r\n/g, ' ');
    escapedTerm = escapedTerm.replace(/\n/g, ' ');
    escapedTerm = escapedTerm.replace(/<br>/g, ' ');

    if (this.isPhrase(escapedTerm)) {
      escapedTerm = String(escapedTerm).replace(/(["\\])/g, '\\$&');
      escapedTerm = `"${escapedTerm}"`;
    } else {
      // Escape all lucene special characters from the source: && || : \ / + - ! ( ) { } [ ] ^ " ~ * ?
      escapedTerm = String(escapedTerm).replace(/(&&|\|\||[:\\/+\-!(){}[\]^"~*?])/g, '\\$&');
    }

    return escapedTerm;
  };

  addToQuery = (oldQuery: string, newTerm: string, operator: string = 'AND') => {
    if (this.queryContainsTerm(oldQuery, newTerm)) {
      return oldQuery;
    }

    if (trim(oldQuery) === '*' || trim(oldQuery) === '') {
      return newTerm;
    }

    return `${oldQuery} ${operator} ${newTerm}`;
  };
}
