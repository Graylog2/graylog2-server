import * as React from 'react';

import { Timestamp } from 'components/common';
import type { IndexSummary } from 'stores/indexers/IndexerOverviewStore';

type Props = {
  index?: IndexSummary;
};

const IndexSetOldestMessageCell = ({ index }: Props) => (
  <Timestamp dateTime={index?.range?.begin} />
);

export default IndexSetOldestMessageCell;
