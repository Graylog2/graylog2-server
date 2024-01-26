/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import { SearchSuggestions } from '@graylog/server-api';

import type { Completer, CompleterContext } from '../SearchBarAutocompletions';

class QueryHistoryCompletion implements Completer {
  // eslint-disable-next-line class-methods-use-this
  getCompletions = ({ tokens, commandArgs }: CompleterContext) => {
    const isShowHistoryCommand = typeof commandArgs === 'object' && 'context' in commandArgs && commandArgs?.context === 'showHistory';
    const inputHasValue = !!tokens?.length;

    if (inputHasValue && !isShowHistoryCommand) {
      return [];
    }

    // display complete history when "show history" command triggered completions,
    // otherwise use default logic to filter completions.
    const displayAlways = inputHasValue && isShowHistoryCommand;
    const matcher = displayAlways ? () => true : undefined;

    return SearchSuggestions.suggestQueryStrings(50)
      .then((response) => response.sort((
        { last_used: lastUsedA }, { last_used: lastUsedB }) => new Date(lastUsedA).getTime() - new Date(lastUsedB).getTime(),
      ).map((entry, index) => ({
        value: entry.query,
        meta: 'history',
        caption: entry.query,
        name: entry.query,
        score: index,
        matcher,
        completer: {
          insertMatch: ({ setValue }: { setValue: (value: string) => void }) => {
            setValue(entry.query);
          },
        },
      })));
  };
}

export default QueryHistoryCompletion;
