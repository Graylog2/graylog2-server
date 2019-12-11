// @flow strict
export type Message = {|
  id: string,
  index: string,
  fields: { [string]: any },
  decoration_stats?: {
    added_fields: { [string]: any },
    changed_fields: { [string]: any },
    removed_fields: { [string]: any },
  },
|};
