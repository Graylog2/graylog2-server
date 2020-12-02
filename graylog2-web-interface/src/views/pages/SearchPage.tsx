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

import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import Search from 'views/components/Search';
import { loadNewView as defaultLoadNewView, loadView as defaultLoadView } from 'views/logic/views/Actions';
import IfUserHasAccessToAnyStream from 'views/components/IfUserHasAccessToAnyStream';

type Props = {
  loadNewView?: () => unknown,
  loadView?: (string) => unknown,
};

const SearchPage = ({ loadNewView = defaultLoadNewView, loadView = defaultLoadView }: Props) => (
  <NewViewLoaderContext.Provider value={loadNewView}>
    <ViewLoaderContext.Provider value={loadView}>
      <IfUserHasAccessToAnyStream>
        <Search />
      </IfUserHasAccessToAnyStream>
    </ViewLoaderContext.Provider>
  </NewViewLoaderContext.Provider>
);

SearchPage.defaultProps = {
  loadNewView: defaultLoadNewView,
  loadView: defaultLoadView,
};

export default SearchPage;
