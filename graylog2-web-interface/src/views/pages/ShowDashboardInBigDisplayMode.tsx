/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React, { useEffect } from 'react';
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
import withLocation, { Location } from 'routing/withLocation';

import ShowViewPage from './ShowViewPage';

type BigDisplayModeQuery = {
  tabs?: Array<number>,
  interval: number,
  refresh: number,
};

type Props = {
  location: Location,
  view: {
    view?: View,
    activeQuery?: QueryId,
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

const ShowDashboardInBigDisplayMode = ({ location, view: { view, activeQuery } = {} }: Props) => {
  const { query } = location;
  const configuration = castQueryWithDefaults(query);

  useEffect(() => {
    RefreshActions.setInterval(configuration.refresh * 1000);
    RefreshActions.enable();

    return () => {
      RefreshActions.disable();
    };
  }, [configuration.refresh]);

  return (
    <InteractiveContext.Provider value={false}>
      <BodyPositioningWrapper>
        {view && activeQuery ? <CycleQueryTab interval={configuration.interval} view={view} activeQuery={activeQuery} tabs={configuration.tabs} /> : null}
        <BigDisplayModeHeader />
        <ShowViewPage />
      </BodyPositioningWrapper>
    </InteractiveContext.Provider>
  );
};

export default withLocation(connect(ShowDashboardInBigDisplayMode, { view: ViewStore }));
