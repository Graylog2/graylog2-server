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

import { Alert } from 'components/bootstrap';

type Props = {
  children: React.ReactNode,
  className?: string,
};

/**
 * Component used to display a simple alert message for a search that returned no matching results.
 * Usage should include utilizing the `children` props to supply the user with a descriptive message.
*/
const NoSearchResult = ({ children, className }: Props) => (
  <Alert className={`${className ?? ''} no-bm`}>
    {children || 'No data available.'}
  </Alert>
);

export default NoSearchResult;
