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
import * as React from 'react';

import { singleton } from 'logic/singleton';
import type { WidgetMapping } from 'views/logic/views/types';

export type WidgetExplain = {
  query_string: string,
  searched_index_ranges: Array<{
    index_name: string,
    begin: number,
    end: number,
    is_warm_tiered: boolean,
    stream_names: Array<string>
  }>
}

type ExplainedSearch = {
    search_id: string,
    search: {
      queries: {
        [key:string]: {
          search_types: {
             [key:string]: WidgetExplain
          }
        }
      }
    },
    search_errors: Array<Object>,
  }

export type SearchExplainContextType = {
  getExplainForWidget: (queryId: string, widgetId: string, widgetMapping: WidgetMapping) => WidgetExplain | undefined,
  explainedSearch: ExplainedSearch,
};

const defaultContext = {
  getExplainForWidget: () => undefined,
  explainedSearch: undefined,
};

const SearchExplainContext = React.createContext<SearchExplainContextType>(defaultContext);

export default singleton('contexts.SearchExplain', () => SearchExplainContext);
