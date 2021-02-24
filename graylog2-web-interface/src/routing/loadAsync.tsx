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
import { Suspense } from 'react';
import PropTypes from 'prop-types';

import Spinner from 'components/common/Spinner';

type Error = {
  message: string;
};

type Props = {
  error: Error;
};

const ErrorComponent: React.ComponentType<Props> = ({ error }: Props) => <div>Loading component failed: {error.message}</div>;

ErrorComponent.propTypes = {
  error: PropTypes.exact({
    message: PropTypes.string,
  }).isRequired,
};

class AsyncLoaderErrorBoundary extends React.Component<{ errorComponent: React.ComponentType<Props> }, { error?: Error }> {
  constructor(props: Readonly<{ errorComponent: React.ComponentType<Props> }>) {
    super(props);
    this.state = {};
  }

  componentDidCatch(error) {
    this.setState({ error });
  }

  render() {
    const { error } = this.state;
    const { children } = this.props;

    if (error) {
      const { errorComponent: CustomErrorComponent } = this.props;

      return <CustomErrorComponent error={error} />;
    }

    return children;
  }
}

export default <T extends React.ComponentType<any>>(f: () => Promise<{ default: T }>) => (props: React.ComponentPropsWithRef<T>) => {
  const LazyComponent = React.lazy(f);

  return (
    <AsyncLoaderErrorBoundary errorComponent={ErrorComponent}>
      <Suspense fallback={<Spinner />}>
        <LazyComponent {...props} />
      </Suspense>
    </AsyncLoaderErrorBoundary>
  );
};
