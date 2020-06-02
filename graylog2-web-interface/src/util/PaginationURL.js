// @flow strict
import URI from 'urijs';

export default (destUrl: string, page: number, perPage: number, query: string, resolve: boolean = true): string => {
  const uri = new URI(destUrl).addSearch('page', page)
    .addSearch('per_page', perPage)
    .addSearch('resolve', resolve.toString());
  if (query) {
    return uri.addSearch('query', encodeURIComponent(query)).toString();
  }
  return uri.toString();
};
