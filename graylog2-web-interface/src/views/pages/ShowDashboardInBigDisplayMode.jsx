// @flow strict
import * as React from 'react';
import { withRouter } from 'react-router';
import styled from 'styled-components';

import connect from 'stores/connect';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import { ViewStore } from 'views/stores/ViewStore';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import { CurrentQueryStore } from 'views/stores/CurrentQueryStore';
import BigDisplayModeHeader from 'views/components/dashboard/BigDisplayModeHeader';
import CycleQueryTab from 'views/components/dashboard/bigdisplay/CycleQueryTab';
import type { QueryId } from 'views/logic/queries/Query';
import View from 'views/logic/views/View';
import ShowViewPage from './ShowViewPage';

export type BigDisplayModeQuery = {|
  tabs?: Array<number>,
  interval?: ?number,
  refresh?: ?number,
|};

export type UntypedBigDisplayModeQuery = {|
  tabs?: string,
  interval?: string,
  refresh?: string,
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
  tabs: tabs !== undefined ? tabs.split(',').map(tab => Number.parseInt(tab, 10)) : undefined,
  interval: interval !== undefined ? Number.parseInt(interval, 10) : undefined,
  refresh: refresh !== undefined ? Number.parseInt(refresh, 10) : undefined,
});

const BodyPositioningWrapper = styled.div`
  margin-top: -45px;
  padding: 10px;
`;

const ShowDashboardInBigDisplayMode = ({ location, params, route, view: { view, activeQuery } }: Props) => {
  const { query } = location;
  const configuration = castQueryWithDefaults(query);
  return (
    <InteractiveContext.Provider value={false}>
      <BodyPositioningWrapper>
        {view && activeQuery ? <CycleQueryTab interval={configuration.interval || 30} view={view} activeQuery={activeQuery} tabs={configuration.tabs} /> : null}
        <BigDisplayModeHeader />
        <ShowViewPage location={location} params={params} route={route} />
      </BodyPositioningWrapper>
    </InteractiveContext.Provider>
  );
};

export default withRouter(connect(ShowDashboardInBigDisplayMode, { view: ViewStore, viewMetadata: ViewMetadataStore, query: CurrentQueryStore }));
