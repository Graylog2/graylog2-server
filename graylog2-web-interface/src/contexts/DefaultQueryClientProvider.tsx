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
import { useMemo } from 'react';
import type { QueryClientConfig } from '@tanstack/react-query';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { merge } from 'lodash';

type Props = {
  children: React.ReactNode,
  options?: QueryClientConfig
};

const defaultOptions = {
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
    },
  },
};

const DefaultQueryClientProvider = ({ children, options: optionsProp }: Props) => {
  const options = optionsProp ? merge({}, defaultOptions, optionsProp) : defaultOptions;
  const queryClient = useMemo(() => new QueryClient(options), [options]);

  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
};

DefaultQueryClientProvider.defaultProps = {
  options: undefined,
};

export default DefaultQueryClientProvider;
