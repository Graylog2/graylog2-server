// @flow strict
import moment from 'moment-timezone';
import CombinedProvider from 'injection/CombinedProvider';

import { QueriesActions } from 'views/stores/QueriesStore';
import Query from 'views/logic/queries/Query';
import type { ViewType } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import { GlobalOverrideActions } from 'views/stores/GlobalOverrideStore';
import SearchActions from 'views/actions/SearchActions';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const onZoom = (currentQuery: Query, from: string, to: string, viewType: ?ViewType) => {
  const { timezone } = CurrentUserStore.get();

  const newTimerange = {
    type: 'absolute',
    from: moment.tz(from, timezone).toISOString(),
    to: moment.tz(to, timezone).toISOString(),
  };

  const action = viewType === View.Type.Dashboard
    ? timerange => GlobalOverrideActions.timerange(timerange).then(SearchActions.refresh)
    : timerange => QueriesActions.timerange(currentQuery.id, timerange);

  action(newTimerange);
  return false;
};

export default onZoom;
