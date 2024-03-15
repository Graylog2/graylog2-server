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
import * as React from 'react';

import { adjustFormat } from 'util/DateTime';
import type { IndexRange } from 'views/components/searchbar/queryvalidation/types';
import { isSearchingWarmTier } from 'views/components/searchbar/queryvalidation/warmTierValidation';

type Props = {
  warmTierIndices: IndexRange[]
}

const WarmTierErrorMessage = ({ warmTierIndices } : Props) => {
  if (!isSearchingWarmTier(warmTierIndices)) return null;

  const formatTimestamp = (timestamp: number) : string => `${adjustFormat(new Date((timestamp)), 'default')}`;

  const timestampInfo = warmTierIndices.map((warmTierIndex) => {
    const begin = formatTimestamp(warmTierIndex.begin);
    const end = formatTimestamp(warmTierIndex.end);

    return `${begin} to ${end}`;
  });

  const timestampString = timestampInfo.join(', ');

  return (
    <span>
      The selected time range includes data stored in the Warm Tier, which can be slow to retrieve.
      {timestampString.length > 0 && (` The following interval falls within the Warm Tier: ${timestampString}.`)}
    </span>
  );
};

export default WarmTierErrorMessage;
