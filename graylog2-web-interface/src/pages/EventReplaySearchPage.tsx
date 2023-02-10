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

import React, { useEffect, useState } from 'react';

import useParams from 'routing/useParams';
import type { EventType } from 'hooks/useEventById';
import useEventById from 'hooks/useEventById';
import useEventDefinition from 'hooks/useEventDefinition';
import { Spinner } from 'components/common';
import SearchPage from 'views/pages/SearchPage';
import useCreateSavedSearch from 'views/logic/views/UseCreateSavedSearch';
import type { EventDefinition } from 'logic/alerts/types';
import { EventNotificationsActions } from 'stores/event-notifications/EventNotificationsStore';

const transformExpresionsToArray = ({ series, conditions }) => {
  const res = [];

  const rec = (expression) => {
    if (!expression) {
      return 'No condition configured';
    }

    switch (expression.expr) {
      case 'number':
        return ({ value: expression.value });
      case 'number-ref':
        // eslint-disable-next-line no-case-declarations
        const selectedSeries = series.find((s) => s.id === expression.ref);

        return (selectedSeries && selectedSeries.function
          ? { field: `${selectedSeries.function}(${selectedSeries.field})` }
          : null);
      case '&&':
      case '||':
        return [rec(expression.left), rec(expression.right)];
      case 'group':
        return [rec(expression.child)];
      case '<':
      case '<=':
      case '>':
      case '>=':
      case '==':
        return [rec(expression.left), rec(expression.right)];
      default:
        return null;
    }
  };

  // rec(conditions.expression);

  return rec(conditions.expression);
};

const EventView = ({ eventData, EDData }: { eventData: EventType, EDData: EventDefinition }) => {
  const view = useCreateSavedSearch(eventData.replay_info.streams, {
    type: 'absolute',
    from: eventData?.replay_info?.timerange_start,
    to: eventData?.replay_info?.timerange_end,
  }, {
    type: 'elasticsearch',
    query_string: eventData?.replay_info?.query || ' ',
  }).then(async (initialView) => {
    // const initState = initialView.state;
    const queryId = initialView.search.queries.first().id;
    const groupBy = EDData.config.group_by;
    const series = EDData.config.series.map(({ function: fn, field }) => `${fn}(${field})`);
    // const state = initialView.state.get(queryId).toBuilder().widgets()
    const fff = transformExpresionsToArray({ series: EDData.config.series, conditions: EDData.config.conditions });
    console.log('!!!!!!$$$$$$$$$$$$$$$$$$$$$$$$!!!!!!', { groupBy, series, EDData, fff });

    return initialView;
  });

  return <SearchPage view={view} isNew />;
};

const EventReplaySearchPage = () => {
  const [isNotificationLoaded, setIsNotificationLoaded] = useState(false);
  const { alertId } = useParams<{ alertId?: string }>();
  const { data: eventData, isLoading: eventIsLoading, refetch: refetchEvent, isFetched: eventIsFetched } = useEventById(alertId);
  const { data: EDData, isLoading: EDIsLoading, refetch: refetchED, isFetched: EDIsFetched } = useEventDefinition(eventData?.event_definition_id);
  useEffect(() => { EventNotificationsActions.listAll().then(() => setIsNotificationLoaded(true)); }, [setIsNotificationLoaded]);
  const isLoading = eventIsLoading || EDIsLoading || !eventIsFetched || !EDIsFetched || !isNotificationLoaded;
  /*
  const view = useMemo(() => {
    if (isLoading) return null;

    const query = Query.builder().newId().query({
      type: 'elasticsearch',
      query_string: eventData.replay_info.query,
    }).timerange({
      type: 'absolute',
      from: eventData.replay_info.timerange_start,
      to: eventData.replay_info.timerange_end,
    })
      .build();
    const search = Search
      .create()
      .toBuilder()
      .queries([query])
      .build();

    const widgets = [MessagesWidget
      .builder()
      .newId()
      .config(MessagesWidgetConfig.builder().build()).build()];

    return View
      .builder()
      .type('SEARCH')
      .search(search)
      .state({ [query.id]: ViewState.create().toBuilder().widgets(widgets).build() })
      .build();
  }, [eventData, isLoading]);
  */

  // const view = useCreateSavedSearch(streams, timeRange, queryString);

  return isLoading ? <Spinner /> : <EventView eventData={eventData} EDData={EDData} />;
};

export default EventReplaySearchPage;
