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
import moment from 'moment-timezone';

import AppConfig from 'util/AppConfig';
import CombinedProvider from 'injection/CombinedProvider';
import { QueriesActions } from 'views/stores/QueriesStore';
import Query from 'views/logic/queries/Query';
import type { ViewType } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import { GlobalOverrideActions } from 'views/stores/GlobalOverrideStore';
import SearchActions from 'views/actions/SearchActions';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const onZoom = (currentQuery: Query, from: string, to: string, viewType: ViewType | undefined | null) => {
  const currentUser = CurrentUserStore.get();
  const timezone = currentUser?.timezone ?? AppConfig.rootTimeZone();

  const newTimerange = {
    type: 'absolute',
    from: moment.tz(from, timezone).toISOString(),
    to: moment.tz(to, timezone).toISOString(),
  };

  const action = viewType === View.Type.Dashboard
    ? (timerange) => GlobalOverrideActions.timerange(timerange).then(SearchActions.refresh)
    : (timerange) => QueriesActions.timerange(currentQuery.id, timerange);

  action(newTimerange);

  return false;
};

export default onZoom;
