export type ElasticsearchQueryString = {
  type: 'elasticsearch';
  query_string: string;
};
export interface PluggableQueryString {
  'elasticsearch': ElasticsearchQueryString;
}

export type QueryString = PluggableQueryString[keyof PluggableQueryString];
