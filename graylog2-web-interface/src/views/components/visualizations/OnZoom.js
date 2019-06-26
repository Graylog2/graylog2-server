// @flow strict
import moment from 'moment-timezone';
import { QueriesActions } from 'views/stores/QueriesStore';
import { SearchActions } from 'views/stores/SearchStore';
import CombinedProvider from 'injection/CombinedProvider';
import Query from 'views/logic/queries/Query';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const onZoom = (currentQuery: Query, from: string, to: string) => {
  const { timezone } = CurrentUserStore.get();

  const newTimerange = {
    type: 'absolute',
    from: moment.tz(from, timezone).toISOString(),
    to: moment.tz(to, timezone).toISOString(),
  };

  QueriesActions.timerange(currentQuery.id, newTimerange).then(SearchActions.executeWithCurrentState);
  return false;
};

export default onZoom;
