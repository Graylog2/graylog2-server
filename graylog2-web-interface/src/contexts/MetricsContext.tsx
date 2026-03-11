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

import { singleton } from 'logic/singleton';
import type { ClusterMetric } from 'types/metrics';

export type MetricsContextType = {
  metrics: ClusterMetric;
  isLoading: boolean;
  subscribe: (names: string[]) => void;
  unsubscribe: (names: string[]) => void;
};

const MetricsContext = React.createContext<MetricsContextType | undefined>(undefined);

export default singleton('contexts.MetricsContext', () => MetricsContext);
