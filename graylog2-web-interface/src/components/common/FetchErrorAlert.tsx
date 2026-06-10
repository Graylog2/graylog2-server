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
  message: string;
  error?: Error | null;
};

/**
 * Renders a danger `Alert` summarizing a failed fetch.
 * Use for user-facing error states where a data fetch rejected and there is no
 * way to recover automatically — e.g. in place of a table body or a chart.
 */
const FetchErrorAlert = ({ message, error = null }: Props) => (
  <Alert bsStyle="danger">
    {message}: {error?.message ?? 'Unknown error'}
  </Alert>
);

export default FetchErrorAlert;
