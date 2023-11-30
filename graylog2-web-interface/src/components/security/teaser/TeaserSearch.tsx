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
import styled from 'styled-components';
import { useMemo } from 'react';

import SearchExecutorsContext from 'views/components/contexts/SearchExecutorsContext';
import { SAVE_COPY } from 'views/components/contexts/SearchPageLayoutContext';
import SearchPage from 'views/pages/SearchPage';
import type { SearchExecutors } from 'views/logic/slices/searchExecutionSlice';
import PageContentLayout from 'components/layout/PageContentLayout';
import SearchPageLayoutProvider from 'views/components/contexts/SearchPageLayoutProvider';
import type { ViewJson } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import type { SearchJobResult } from 'views/logic/SearchResult';
import SearchResult from 'views/logic/SearchResult';
import type { SearchJson } from 'views/logic/search/Search';
import Search from 'views/logic/search/Search';
import SearchMetadata from 'views/logic/search/SearchMetadata';
import Hotspot from 'components/security/teaser/Hotspot';

type HotspotMeta = {
  positionX: string,
  positionY: string,
  description: string
}

const StyledPageContentLayout = styled(PageContentLayout)`
  .page-content-grid {
    position: relative;
  }
`;

const DashboardOverlay = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  background: transparent;
  z-index: 1;
`;

const searchAreaContainer = (hotspots: Array<HotspotMeta>) => ({ children }: React.PropsWithChildren) => (
  <StyledPageContentLayout>
    <DashboardOverlay>
      {hotspots.map(({ description, positionX, positionY }, index) => (
        <Hotspot positionX={positionX} positionY={positionY} index={index}>
          {description}
        </Hotspot>
      ))}
    </DashboardOverlay>
    <div inert="">
      {children}
    </div>
  </StyledPageContentLayout>
);

const searchMetadata = SearchMetadata.empty();

type Props = {
  searchJson: Partial<SearchJson>,
  viewJson: Partial<ViewJson>,
  searchJobResult: Partial<SearchJobResult>,
  hotspots: Array<HotspotMeta>,
}

const TeaserSearch = ({ searchJson, viewJson, searchJobResult, hotspots }: Props) => {
  const view = useMemo(() => {
    const search = Search.fromJSON(searchJson as SearchJson);

    return View.fromJSON(viewJson as ViewJson)
      .toBuilder()
      .search(search)
      .build();
  }, [searchJson, viewJson]);
  const searchResult = useMemo(() => ({
    result: new SearchResult(searchJobResult as SearchJobResult),
    widgetMapping: view.widgetMapping,
  }), [searchJobResult, view.widgetMapping]);

  const searchExecutors: SearchExecutors = useMemo(() => ({
    execute: async () => searchResult,
    parse: async () => searchMetadata,
    resultMapper: (result) => result,
  }), [searchResult]);

  const searchPageLayout = useMemo(() => ({
    sidebar: { isShown: false },
    viewActions: SAVE_COPY,
    searchAreaContainer: { component: searchAreaContainer(hotspots) },
  }), [hotspots]);

  return (
    <SearchPageLayoutProvider value={searchPageLayout}>
      <SearchExecutorsContext.Provider value={searchExecutors}>
        <SearchPage view={Promise.resolve(view)} isNew={false} searchResult={searchResult} />
      </SearchExecutorsContext.Provider>
    </SearchPageLayoutProvider>
  );
};

export default TeaserSearch;
