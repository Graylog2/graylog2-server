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
import { useEffect } from 'react';
import { ErrorBoundary } from 'react-error-boundary';

type Props = {
  error: Error;
};

const ErrorComponent = ({ error }: Props) => {
  useEffect(() => {
    // eslint-disable-next-line no-console
    console.error(error);
  }, [error]);

  return <div>Loading component failed: {error.message}</div>;
};

type ComponentSupplier<TProps> = () => Promise<{ default: React.ComponentType<TProps> }>;

// eslint-disable-next-line react/jsx-no-useless-fragment
const emptyPlaceholder = <></>;

const loadAsync = <TProps, >(factory: ComponentSupplier<TProps>): React.ComponentType<JSX.IntrinsicAttributes & React.PropsWithoutRef<TProps>> => {
  const Component = React.lazy(factory);

  return (props: JSX.IntrinsicAttributes & React.PropsWithoutRef<TProps>) => (
    <ErrorBoundary FallbackComponent={ErrorComponent}>
      <React.Suspense fallback={emptyPlaceholder}>
        <Component {...props} />
      </React.Suspense>
    </ErrorBoundary>
  );
};

export default loadAsync;
