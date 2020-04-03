// @flow strict
import React, { useEffect } from 'react';
import { withRouter } from 'react-router';
import styled from 'styled-components';

import connect from 'stores/connect';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import { ViewStore } from 'views/stores/ViewStore';
import BigDisplayModeHeader from 'views/components/dashboard/BigDisplayModeHeader';
import CycleQueryTab from 'views/components/dashboard/bigdisplay/CycleQueryTab';
import type { QueryId } from 'views/logic/queries/Query';
import View from 'views/logic/views/View';
import { RefreshActions } from 'views/stores/RefreshStore';
import type { UntypedBigDisplayModeQuery } from 'views/components/dashboard/BigDisplayModeConfiguration';
import ShowViewPage from './ShowViewPage';

type BigDisplayModeQuery = {|
  tabs: ?Array<number>,
  interval: number,
  refresh: number,
|};

type Props = {
  location: {
    query: UntypedBigDisplayModeQuery,
  },
  params: any,
  route: any,
  view: {
    view: ?View,
    activeQuery: ?QueryId,
  },
};

const castQueryWithDefaults = ({ tabs, interval, refresh }: UntypedBigDisplayModeQuery): BigDisplayModeQuery => ({
  tabs: tabs !== undefined ? tabs.split(',').map((tab) => Number.parseInt(tab, 10)) : undefined,
  interval: interval !== undefined ? Math.max(Number.parseInt(interval, 10), 1) : 30,
  refresh: refresh !== undefined ? Math.max(Number.parseInt(refresh, 10), 1) : 10,
});

const BodyPositioningWrapper = styled.div`
  margin-top: -45px;
  padding: 10px;
`;

const ShowDashboardInBigDisplayMode = ({ location, params, route, view: { view, activeQuery } = {} }: Props) => {
  const { query } = location;
  const configuration = castQueryWithDefaults(query);
  useEffect(() => {
    RefreshActions.setInterval(configuration.refresh * 1000);
    RefreshActions.enable();
    return () => RefreshActions.disable();
  }, [configuration.refresh]);
  return (
    <InteractiveContext.Provider value={false}>
      <BodyPositioningWrapper>
        {view && activeQuery ? <CycleQueryTab interval={configuration.interval} view={view} activeQuery={activeQuery} tabs={configuration.tabs} /> : null}
        <BigDisplayModeHeader />
        <ShowViewPage location={location} params={params} route={route} />
      </BodyPositioningWrapper>
    </InteractiveContext.Provider>
  );
};

export default withRouter(connect(ShowDashboardInBigDisplayMode, { view: ViewStore }));
