import React from 'react';

import usePluginEntities from 'hooks/usePluginEntities';

export const usePluggableEventActions = (eventId: string) => {
  const pluggableEventActions = usePluginEntities('views.components.eventActions');

  return pluggableEventActions.filter(
    (perspective) => (perspective.useCondition ? !!perspective.useCondition() : true),
  ).map(
    ({ component: PluggableEventAction, key }) => (
      <PluggableEventAction key={key} eventId={eventId} />
    ),
  );
};

export default usePluggableEventActions;
