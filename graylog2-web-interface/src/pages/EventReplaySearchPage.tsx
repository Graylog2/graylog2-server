import React from 'react';

import useParams from 'routing/useParams';
import type { EventType } from 'hooks/useEventById';
import useEventById from 'hooks/useEventById';
import useEventDefinition from 'hooks/useEventDefinition';
import { Spinner } from 'components/common';
import SearchPage from 'views/pages/SearchPage';
import useCreateSavedSearch from 'views/logic/views/UseCreateSavedSearch';
import type { EventDefinition } from 'logic/alerts/types';

const transformExpresionsToArray = ({ series, conditions }) => {
  const res = [];

  const rec = (expression) => {
    if (!expression) {
      return 'No condition configured';
    }

    switch (expression.expr) {
      case 'number':
        return expression.value;
      case 'number-ref':
        // eslint-disable-next-line no-case-declarations
        const selectedSeries = series.find((s) => s.id === expression.ref);

        return (selectedSeries && selectedSeries.function
          ? `${selectedSeries.function}(${selectedSeries.field})`
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
    const fff = transformExpresionsToArray({ series, conditions: EDData.config.conditions });
    console.log('!!!!!!$$$$$$$$$$$$$$$$$$$$$$$$!!!!!!', { groupBy, series, EDData, fff });

    return initialView;
  });

  return <SearchPage view={view} isNew />;
};

const EventReplaySearchPage = () => {
  const { alertId } = useParams<{ alertId?: string }>();
  const { data: eventData, isLoading: eventIsLoading, refetch: refetchEvent, isFetched: eventIsFetched } = useEventById(alertId);
  const { data: EDData, isLoading: EDIsLoading, refetch: refetchED, isFetched: EDIsFetched } = useEventDefinition(eventData?.event_definition_id);

  const isLoading = eventIsLoading || EDIsLoading || !eventIsFetched || !EDIsFetched;
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
