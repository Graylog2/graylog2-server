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
import React from 'react';
import styled from 'styled-components';

import { Alert } from 'components/bootstrap';
import type FetchError from 'logic/errors/FetchError';

const ESClusterError = styled(Alert)`
  margin-top: 10px;
  margin-bottom: 5px;
`;

const IndexerClusterHealthError = ({ error, name }: { error: FetchError, name?: { name: string, distribution: string } }) => (
  <ESClusterError bsStyle="danger">
    Could not retrieve {name?.distribution || 'Elasticsearch'} cluster health. Fetching {name?.distribution || 'Elasticsearch'} cluster health failed: {error.message}
  </ESClusterError>
);

export default IndexerClusterHealthError;
