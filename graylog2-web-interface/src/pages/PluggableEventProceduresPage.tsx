import * as React from 'react';

import usePluginEntities from 'hooks/usePluginEntities';
import EventsPageNavigation from 'components/events/EventsPageNavigation';

const PluggableEventProceduresPage = () => {
  const pluggableEventProcedures = usePluginEntities('eventProcedures');

  const EventProceduresPage = pluggableEventProcedures[0]?.EventProcedures;

  return (
    <EventProceduresPage navigationComponent={<EventsPageNavigation />} useCoreRoutes />
  );
};

export default PluggableEventProceduresPage;
