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

import type { PaginationQueryParameterResult } from 'hooks/usePaginationQueryParameter';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';

export type PaginationQueryParameterObject = {
  pageSizes?: number[];
};

export type PaginationProps = {
  paginationQueryParameter: PaginationQueryParameterResult;
};

const withPaginationQueryParameter = <Props extends PaginationProps>(Component: React.ComponentType<Props>, obj?: PaginationQueryParameterObject) => (function WrappedComponent(props: Omit<Props, keyof PaginationProps>) {
  const result = usePaginationQueryParameter(obj?.pageSizes);

  return <Component {...props as Props} paginationQueryParameter={result} />;
});

export default withPaginationQueryParameter;
