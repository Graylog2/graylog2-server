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

const ErrorsContext = React.createContext(null);

type ProviderProps = {
  children: React.ReactNode;
};

export function ErrorsProvider({ children }: ProviderProps) {
  const [errors, setErrors] = React.useState<{ lutErrors: unknown; cacheErrors: unknown; adapterErrors: unknown }>();
  const value = React.useMemo(() => ({ errors, setErrors }), [errors, setErrors]);

  return <ErrorsContext.Provider value={value}>{children}</ErrorsContext.Provider>;
}

export function useErrorsContext() {
  return React.useContext(ErrorsContext);
}
