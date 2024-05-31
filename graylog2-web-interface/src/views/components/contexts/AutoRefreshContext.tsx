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

export type RefreshConfig = {
  interval: number,
  enabled: boolean
}

export type IntervalSetupCallback = () => void;

type AutoRefreshContextType = {
  refreshConfig: RefreshConfig | null,
  startAutoRefresh: (interval: number) => void
  stopAutoRefresh: () => void,
  registerIntervalSetupCallback: (callback: IntervalSetupCallback, id: string) => void,
  unregisterIntervalSetupCallback: (id: string) => void,
};

const AutoRefreshContext = React.createContext<AutoRefreshContextType | null>(null);

export default singleton('contexts.AutoRefreshContext', () => AutoRefreshContext);
