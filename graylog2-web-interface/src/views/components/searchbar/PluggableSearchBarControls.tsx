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
import { useState } from 'react';

import usePluginEntities from 'views/logic/usePluginEntities';
import SearchFilterBanner from 'views/components/searchbar/SearchFilterBanner';
import useFeature from 'hooks/useFeature';
import type { SearchBarControl } from 'views/types';
import Store from 'logic/local-storage/Store';

const LOCAL_STORAGE_ITEM = 'search_filter_preview_viewed';

const Container = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
`;

const PluggableSearchBarControls = () => {
  const [isBannerHidden, setIsBannerHidden] = useState(Store.get(LOCAL_STORAGE_ITEM));
  const searchBarControls = usePluginEntities('views.components.searchBar');
  const hasSearchFilterFeature = useFeature('search_filter');
  const existingControls = searchBarControls.map((controlFn) => controlFn()).filter((control) => !!control);
  const leftControls = existingControls.filter(({ placement }) => placement === 'left');
  const rightControls = existingControls.filter(({ placement }) => placement === 'right');
  const renderControls = (controls: Array<SearchBarControl>) => controls?.map(({ component: ControlComponent, id }) => <ControlComponent key={id} />);
  const hasEnterpriseSearchFilters = existingControls.find((control) => control.id === 'search-filters');
  const shouldRenderContainer = (hasEnterpriseSearchFilters || !isBannerHidden) && hasSearchFilterFeature;

  if (shouldRenderContainer) return null;

  return (
    <Container>
      <div>
        {hasSearchFilterFeature && (
          <>
            {renderControls(leftControls)}
            {!hasEnterpriseSearchFilters && <SearchFilterBanner setHidden={setIsBannerHidden} />}
          </>
        )}
      </div>
      <div>{renderControls(rightControls)}</div>
    </Container>
  );
};

export default PluggableSearchBarControls;
