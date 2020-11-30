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
// @flow strict
import { SearchActions } from 'views/stores/SearchStore';
import { ViewActions } from 'views/stores/ViewStore';
import Search from 'views/logic/search/Search';
import type { SearchJson } from 'views/logic/search/Search';

import View from './View';
import type { ViewJson } from './View';

export default function ViewDeserializer(viewResponse: ViewJson): Promise<View> {
  const view: View = View.fromJSON(viewResponse);

  return SearchActions.get(viewResponse.search_id)
    .then((search: SearchJson): Search => Search.fromJSON(search))
    .then((search: Search): View => view.toBuilder().search(search).build())
    .then((v: View): Promise<View> => ViewActions.load(v).then(() => v));
}
