import React from 'react';

import Routes from 'routing/Routes';
import { ReplaySearchButtonComponent } from 'views/components/widgets/ReplaySearchButton';
import useParams from 'routing/useParams';

const LinkToReplaySearch = ({ isEvent, id }: { id?: string, isEvent?: boolean }) => {
  const { definitionId } = useParams<{ alertId?: string, definitionId?: string }>();
  const searchLink = isEvent ? Routes.ALERTS.replay_search(id) : Routes.ALERTS.DEFINITIONS.replay_search(id || definitionId);

  return (
    <ReplaySearchButtonComponent searchLink={searchLink}>Replay
      search
    </ReplaySearchButtonComponent>
  );
};

LinkToReplaySearch.defaultProps = {
  id: undefined,
  isEvent: false,
};

export default LinkToReplaySearch;
