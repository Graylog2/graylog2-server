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

import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import { indicesInWarmTier, isSearchingWarmTier } from 'views/components/searchbar/queryvalidation/warmTierValidation';
import { Explanation } from 'views/components/searchbar/queryvalidation/QueryValidation';
import WarmTierErrorMessage from 'views/components/searchbar/queryvalidation/WarmTierErrorMessage';

type Props = {
  validationState: QueryValidationState;
};

const WarmTierQueryValidation = ({ validationState } : Props) => {
  const warmTierIndices = indicesInWarmTier(validationState);

  if (!isSearchingWarmTier(warmTierIndices)) return null;

  return (
    <Explanation>
      <span><b>Warm Tier Search</b>: <WarmTierErrorMessage warmTierIndices={warmTierIndices} /></span>
    </Explanation>
  );
};

export default WarmTierQueryValidation;
