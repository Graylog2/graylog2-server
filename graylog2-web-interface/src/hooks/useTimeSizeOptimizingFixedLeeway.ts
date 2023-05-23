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
import { useEffect } from 'react';

import { useStore } from 'stores/connect';
import { IndicesConfigurationActions, IndicesConfigurationStore } from 'stores/indices/IndicesConfigurationStore';

const useTimeSizeOptimizingFixedLeeway = () => {
  const { rotationStrategiesContext } = useStore(IndicesConfigurationStore);
  const maxTimeSizeOptimizingFixedLeeway = rotationStrategiesContext?.time_size_optimizing_retention_fixed_leeway;

  useEffect(() => {
    IndicesConfigurationActions.loadRotationStrategies();
  }, []);

  return maxTimeSizeOptimizingFixedLeeway;
};

export default useTimeSizeOptimizingFixedLeeway;
