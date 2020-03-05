// @flow strict

export type SearchesConfig = {
  surrounding_timerange_options: { [string]: string },
  surrounding_filter_fields: Array<string>,
  query_time_range_limit: string,
  relative_timerange_options: { [string]: string },
  analysis_disabled_fields: Array<string>,
};
