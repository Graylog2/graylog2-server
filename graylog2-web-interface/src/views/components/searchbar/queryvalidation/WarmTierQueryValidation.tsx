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
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import { indicesInWarmTier, isSearchingWarmTier } from 'views/components/searchbar/queryvalidation/warmTierValidation';
import { Explanation } from 'views/components/searchbar/queryvalidation/QueryValidation';

type Props = {
  validationState: QueryValidationState;
};

const WarmTierQueryValidation = ({ validationState } : Props) => {
  const warmTierIndices = indicesInWarmTier(validationState);

  const warmTierErrorMessage = () => {
    if (!isSearchingWarmTier(warmTierIndices)) return null;

    const formatTimestamp = (timestamp: number) : string => `${adjustFormat(new Date((timestamp)), 'default')}`;

    const timestampInfo = warmTierIndices.map((warmTierIndex) => {
      const begin = formatTimestamp(warmTierIndex.begin);
      const end = formatTimestamp(warmTierIndex.end);

      return `${begin} to ${end}`;
    });

    const timestampString = timestampInfo.join(', ');

    let errorMessage = 'The selected time range includes data stored in the Warm Tier, which can be slow to retrieve.';

    if (timestampString.length > 0) {
      errorMessage += ` The following interval falls within the Warm Tier: ${timestampString}.`;
    }

    return errorMessage;
  };

  const errorMessage = warmTierErrorMessage();

  if (!errorMessage) return null;

  return (
    <Explanation>
      <span><b>Warm Tier Search</b>: {errorMessage}</span>
    </Explanation>
  );
};

export default WarmTierQueryValidation;
