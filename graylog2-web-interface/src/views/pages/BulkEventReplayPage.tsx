import * as React from 'react';

import useLocation from 'routing/useLocation';

export type BulkEventReplayState = {
  eventIds: Array<string>;
}

const BulkEventReplayPage = () => {
  const location = useLocation<BulkEventReplayState>();

  return (<span>Foo: {JSON.stringify(location.state)}</span>);
};

export default BulkEventReplayPage;
