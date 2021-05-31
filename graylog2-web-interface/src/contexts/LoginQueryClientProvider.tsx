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
import { QueryClient, QueryClientProvider } from 'react-query';

type Props = {
  children: React.ReactNode,
};

const options = {
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
    },
  },
};

/*
 * Use a separate client provider from the default one to be able to clear cached data once the user
 * logs out. Login page is also created by another component than the regular app.
 */

const LoginQueryClientProvider = ({ children }: Props) => {
  const queryClient = useMemo(() => new QueryClient(options), []);

  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
};

export default LoginQueryClientProvider;
