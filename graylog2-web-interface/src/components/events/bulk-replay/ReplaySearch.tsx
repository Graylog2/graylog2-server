import * as React from 'react';
import { useMemo } from 'react';

import useCreateSearch from 'views/hooks/useCreateSearch';
import EventInfoBar from 'components/event-definitions/replay-search/EventInfoBar';
import SearchPageLayoutProvider from 'views/components/contexts/SearchPageLayoutProvider';
import SearchPage from 'views/pages/SearchPage';
import useCreateViewForEvent from 'views/logic/views/UseCreateViewForEvent';
import useEventDefinition from 'hooks/useEventDefinition';
import Spinner from 'components/common/Spinner';
import type { Event } from 'components/events/events/types';

type Props = {
  event: Event;
}

const ViewReplaySearch = ({ eventData, eventDefinition, aggregations }) => {
  const _view = useCreateViewForEvent({ eventData, eventDefinition, aggregations });
  const view = useCreateSearch(_view);
  const searchPageLayout = useMemo(() => ({
    sidebar: {
      isShown: false,
    },
    infoBar: { component: EventInfoBar },
  }), []);

  return (
    <SearchPageLayoutProvider value={searchPageLayout}>
      <SearchPage view={view} isNew />
    </SearchPageLayoutProvider>
  );
};

const ReplaySearch = ({ event }: Props) => {
  const { data, isLoading } = useEventDefinition(event.event_definition_id);

  return isLoading ? <Spinner />
    : <ViewReplaySearch eventData={event} eventDefinition={data?.eventDefinition} aggregations={data?.aggregations} />;
};

export default ReplaySearch;
