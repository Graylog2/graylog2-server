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

import useCreateSavedSearch from 'views/logic/views/UseCreateSavedSearch';
import { useSearchURLQueryParams } from 'views/logic/NormalizeSearchURLQueryParams';
import useCreateSearch from 'views/hooks/useCreateSearch';

import SearchPage from './SearchPage';

const NewSearchPage = () => {
  const { timeRange, queryString, streams } = useSearchURLQueryParams();
  const viewPromise = useCreateSavedSearch({ streamId: streams, timeRange, queryString });
  const view = useCreateSearch(viewPromise);

  return <SearchPage view={view} isNew />;
};

export default NewSearchPage;
