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
import { useEffect } from 'react';

import type View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';

const createSearchUrl = qualifyUrl('/views/search');

const createSearch = (search: Search) => fetch('POST', createSearchUrl, JSON.stringify(search))
  .then((response) => Search.fromJSON(response));

const useLoadView = (viewPromise: Promise<View>, isNew: boolean) => {
  useEffect(() => {
    viewPromise.then((view) => {
      if (isNew) {
        return createSearch(view.search);
      }

      return Promise.resolve(view.search);
    });
  }, [isNew, viewPromise]);
};

export default useLoadView;
