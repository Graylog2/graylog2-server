import { trim } from 'lodash';
import { QueriesStore } from 'enterprise/stores/QueriesStore';

export default class QueryManipulationHandler {
  constructor() {
    QueriesStore.listen((queries) => { this.queries = queries; });
  }

  queryContainsTerm = (query, termInQuestion) => {
    return query.indexOf(termInQuestion) !== -1;
  };

  isPhrase = (searchTerm) => {
    return String(searchTerm).indexOf(' ') !== -1;
  };

  escape = (searchTerm) => {
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

  addToQuery = (oldQuery, newTerm, operator = 'AND') => {
    if (this.queryContainsTerm(oldQuery, newTerm)) {
      return oldQuery;
    }

    if (trim(oldQuery) === '*' || trim(oldQuery) === '') {
      return newTerm;
    }

    return `${oldQuery} ${operator} ${newTerm}`;
  };
}
