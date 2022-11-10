export type Event = {
  'id': string,
  'event_definition_id': string,
  'event_definition_type': string,
  'priority': string,
  'timestamp': string,
  'timerange_start': string,
  'timerange_end': string,
  'key': string,
  'fields': Object[],
  'group_by_fields': Object[],
  'source_streams': string[],
  'query': string
};

export type EventDefinitionContext = {
  'id': string,
  'title': string,
};
