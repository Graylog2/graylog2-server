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
import { useTranslation } from 'react-i18next';

import type { IndexSummary } from 'hooks/useIndexerOverview';
import IndexSection from 'components/indices/IndexSection';
import type { IndexInfo } from 'hooks/useIndices';

type Props = {
  indexDetails: Array<IndexInfo>;
  indices: Array<IndexSummary>;
  indexSetId?: string;
};

const IndicesOverview = ({ indexDetails, indices, indexSetId = undefined }: Props) => {
  const { t } = useTranslation('indexsets');
  const indicesFilteredByTier = (
    indicesList: Array<IndexSummary>,
    tier: 'WARM' | 'HOT' | undefined,
  ): Array<IndexSummary> => indicesList.filter((index) => index.tier === tier);

  const warmTierIndices = indicesFilteredByTier(indices, 'WARM');
  const hotTierIndices = indicesFilteredByTier(indices, 'HOT');
  const hotTierList = hotTierIndices.length > 0 ? hotTierIndices : indicesFilteredByTier(indices, undefined);
  const hotTierSubheading = t('overview.tiers.hot_description');
  const warmTierSubheading = t('overview.tiers.warm_description');

  return (
    <>
      <IndexSection
        headline={t('overview.tiers.hot_title')}
        subheading={hotTierSubheading}
        indices={hotTierList}
        indexDetails={indexDetails}
        indexSetId={indexSetId}
      />
      {warmTierIndices.length > 0 && (
        <IndexSection
          headline={t('overview.tiers.warm_title')}
          subheading={warmTierSubheading}
          indices={warmTierIndices}
          indexDetails={indexDetails}
          indexSetId={indexSetId}
        />
      )}
    </>
  );
};

export default IndicesOverview;
