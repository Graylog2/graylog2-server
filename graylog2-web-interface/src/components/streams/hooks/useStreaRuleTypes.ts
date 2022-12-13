import { useQuery } from '@tanstack/react-query';

import type { StreamRuleType } from 'stores/streams/StreamsStore';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';
import UserNotification from 'util/UserNotification';

const useStreamRuleTypes = (): { data: Array<StreamRuleType> | undefined } => {
  const { data } = useQuery(
    ['streams', 'rule-types'],
    () => StreamRulesStore.types(),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading stream rule types failed with status: ${errorThrown}`,
          'Could not load stream rule types');
      },
      keepPreviousData: true,
    },
  );

  return ({ data });
};

export default useStreamRuleTypes;
