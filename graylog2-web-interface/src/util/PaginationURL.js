// @flow strict
import URI from 'urijs';

type AdditionalQueries = { [string]: any };

export default (destUrl: string, page: number, perPage: number, query: string, additional: AdditionalQueries = {}): string => {
  let uri = new URI(destUrl).addSearch('page', page)
    .addSearch('per_page', perPage);
  if (additional) {
    Object.keys(additional).forEach((field) => {
      const value = (additional[field] && typeof additional[field].toString === 'function')
        ? additional[field].toString()
        : additional[field];
      uri = uri.addSearch(field, value);
    });
  }
  if (query) {
    uri.addSearch('query', encodeURIComponent(query));
  }
  return uri.toString();
};
