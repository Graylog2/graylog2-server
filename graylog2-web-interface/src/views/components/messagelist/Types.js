// @flow strict
import * as Immutable from 'immutable';

import type { AbsoluteTimeRange } from 'views/logic/queries/Query';

export type Message = {|
  id: string,
  index: string,
  fields?: { [string]: any },
  formatted_fields?: { [string]: any },
  highlight_ranges?: { [string]: any },
  decoration_stats?: {
    added_fields: { [string]: any },
    changed_fields: { [string]: any },
    removed_fields: { [string]: any },
  },
  message: {
    streams: Array<string>,
    [string]: number | string,
  },
|};

export type MessageListResult = {
  effectiveTimerange: AbsoluteTimeRange,
  fields: Immutable.Map<string, string>,
  messages: Array<{
    message: Message,
    index: string,
  }>,
  id: string,
  total: number,
  type: 'message',
};

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
