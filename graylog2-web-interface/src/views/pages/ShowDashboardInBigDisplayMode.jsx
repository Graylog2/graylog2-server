// @flow strict
import * as React from 'react';
import { withRouter } from 'react-router';

export type BigDisplayModeQuery = {|
  cycle?: ?boolean,
  tabs?: string,
  interval?: ?number,
  refresh?: ?number,
|};

export type UntypedBigDisplayModeQuery = {|
  cycle?: string,
  tabs?: string,
  interval?: string,
  refresh?: string,
|};

type Props = {
  location: {
    query: UntypedBigDisplayModeQuery,
  },
};

const castQueryWithDefaults = ({ cycle, tabs, interval, refresh }: UntypedBigDisplayModeQuery): BigDisplayModeQuery => ({
  cycle: cycle !== undefined ? JSON.parse(cycle) : false,
  tabs: tabs !== undefined ? tabs.split(',').map(tab => Number.parseInt(tab, 10)) : undefined,
  interval: interval !== undefined ? Number.parseInt(interval, 10) : undefined,
  refresh: refresh !== undefined ? Number.parseInt(refresh, 10) : undefined,
});

const ShowDashboardInBigDisplayMode = ({ location: { query } }: Props) => (
  <div>{JSON.stringify(castQueryWithDefaults(query))}</div>
);

export default withRouter(ShowDashboardInBigDisplayMode);
