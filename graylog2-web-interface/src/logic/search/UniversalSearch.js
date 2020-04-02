import $ from 'jquery';

let initialized = false;

const UniversalSearch = {
  init() {
    if (initialized) {
      return;
    }
    $('#scroll-to-search-hint, #scroll-to-search-hint i').on('click', () => {
      $('html, body').animate({ scrollTop: 0 }, 'fast');
    });
    initialized = true;
  },
  escape(source) {
    // Escape all lucene special characters from the source: && || : \ / + - ! ( ) { } [ ] ^ " ~ * ?
    return source.replace(/(&&|\|\||[:\\\/\+\-!\(\)\{\}\[\]\^"~*\?])/g, '\\$&');
  },
  _query() {
    const query = $('#universalsearch-query');
    return query;
  },
  setQuery(search) {
    const query = this._query();
    query.val(search);
    query.effect('bounce');
    this.scrollToSearchbarHint();
  },
  getQuery() {
    return this._query().val();
  },
  submit() {
    $('#universalsearch form').submit();
  },
  createSourceQuery(source) {
    return `source:${this.escape(source)}`;
  },
  queryContainsSegment(segmentInQuestion) {
    // this may look too complicated, but avoids false positives when one segment would be the prefix of another
    const oldQuery = this.getQuery();
    const segments = oldQuery.split(' ');
    return segments.some((segment) => segment === segmentInQuestion);
  },
  addSegment(segment, operator) {
    let oldQuery = this.getQuery();
    if (this.queryContainsSegment(segment)) {
      return;
    }
    if (oldQuery === '*') {
      oldQuery = '';
    }
    let newQuery = '';
    if (typeof operator !== 'undefined' && oldQuery !== '') {
      newQuery = `${oldQuery} ${operator} `;
    }
    newQuery += segment;
    this.setQuery(newQuery);
  },
  andOperator() {
    return 'AND';
  },
  orOperator() {
    return 'OR';
  },
  notOperator() {
    return 'NOT';
  },
  scrollToSearchbarHint() {
    if ($(document).scrollTop() > 50) {
      $('#scroll-to-search-hint').fadeIn('fast')
        .delay(1500)
        .fadeOut('fast');
    }
  },
  substringMatcher(possibleMatches, displayKey, limit) {
    return function findMatches(q, callback) {
      const matches = [];

      // code duplication is better than a shitty abstraction
      possibleMatches.forEach((possibleMatch) => {
        if (matches.length < limit && possibleMatch.indexOf(q) === 0) {
          const match = {};
          match[displayKey] = possibleMatch;
          matches.push(match);
        }
      });

      possibleMatches.forEach((possibleMatch) => {
        if (matches.length < limit && possibleMatch.indexOf(q) !== -1 && possibleMatch.indexOf(q) !== 0) {
          const match = {};
          match[displayKey] = possibleMatch;
          matches.push(match);
        }
      });

      callback(matches);
    };
  },
};

export default UniversalSearch;
