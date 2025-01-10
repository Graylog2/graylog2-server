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
import React from 'react';

import type { IndexSummary } from 'stores/indexers/IndexerOverviewStore';
import { IndexSection } from 'components/indices';
import type { IndexInfo } from 'stores/indices/IndicesStore';

type Props = {
  indexDetails: Array<IndexInfo>,
  indices: Array<IndexSummary>
  indexSetId: string,
}

const IndicesOverview = ({ indexDetails, indices, indexSetId }: Props) => {
  const indicesFilteredByTier = (indicesList: Array<IndexSummary>, tier: 'WARM' | 'HOT' | undefined): Array<IndexSummary> => (
    indicesList.filter((index) => index.tier === tier)
  );

  const warmTierIndices = indicesFilteredByTier(indices, 'WARM');
  const hotTierIndices = indicesFilteredByTier(indices, 'HOT');
  const hotTierList = hotTierIndices.length > 0 ? hotTierIndices : indicesFilteredByTier(indices, undefined);
  const hotTierSubheading = 'Indices in this section are stored as active shards within the Search cluster, facilitating fast retrieval and search jobs. Data held in the Hot Tier has a permanent footprint in the Java Heap memory of the Search Cluster and in excess, will degrade search performance.';
  const warmTierSubheading = 'Indices in this section are stored as searchable snapshots within the Warm Tier Repository, facilitating cheap storage and low resource overheads. Retrieval and Search jobs of data held in the Warm Tier will be slower. Note that only Search nodes with the "Search" role can participate in Warm Tier search and retrieval.';

  return (
    <>
      <IndexSection headline="Hot Tier"
                    subheading={hotTierSubheading}
                    indices={hotTierList}
                    indexDetails={indexDetails}
                    indexSetId={indexSetId} />
      {warmTierIndices.length > 0 && (
        <IndexSection headline="Warm Tier"
                      subheading={warmTierSubheading}
                      indices={warmTierIndices}
                      indexDetails={indexDetails}
                      indexSetId={indexSetId} />
      )}
    </>
  );
};

export default IndicesOverview;
