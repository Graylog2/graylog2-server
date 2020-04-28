// @flow strict
export type Message = {|
  id: string,
  index: string,
  fields: { [string]: any },
  formatted_fields?: { [string]: any },
  highlight_ranges?: { [string]: any },
  decoration_stats?: {
    added_fields: { [string]: any },
    changed_fields: { [string]: any },
    removed_fields: { [string]: any },
  },
|};

export type BackendMessage = {|
  index: string,
  message: {
    _id: string,
    [string]: mixed,
  },
  highlight_ranges?: { [string]: any },
  decoration_stats?: {
    added_fields: { [string]: any },
    changed_fields: { [string]: any },
    removed_fields: { [string]: any },
  },
|};
