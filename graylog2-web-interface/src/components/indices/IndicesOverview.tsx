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
import PropTypes from 'prop-types';
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
  const indicesFilteredByTier = (indicesList: Array<IndexSummary>, tier: 'warm' | 'hot' | undefined): Array<IndexSummary> => (
    indicesList.filter((index) => index.tier === tier)
  );

  const warmTierIndices = indicesFilteredByTier(indices, 'warm');
  const hotTierIndices = indicesFilteredByTier(indices, 'hot');
  const hotTierList = hotTierIndices.length > 0 ? hotTierIndices : indicesFilteredByTier(indices, undefined);

  return (
    <>
      <IndexSection headline="Hot Tier"
                    subheading="Hot Tier subheading TODO"
                    indices={hotTierList}
                    indexDetails={indexDetails}
                    indexSetId={indexSetId} />
      {warmTierIndices.length > 0 && (
        <IndexSection headline="Warm Tier"
                      subheading="Warm Tier subheading TODO"
                      indices={warmTierIndices}
                      indexDetails={indexDetails}
                      indexSetId={indexSetId} />
      )}
    </>
  );
};

IndicesOverview.propTypes = {
  indexDetails: PropTypes.array.isRequired,
  indices: PropTypes.array.isRequired,
  indexSetId: PropTypes.string.isRequired,
};

export default IndicesOverview;
