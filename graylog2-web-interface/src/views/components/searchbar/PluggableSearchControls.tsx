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

import usePluginEntities from 'views/logic/usePluginEntities';
import SearchFilterBanner from 'views/components/searchbar/SearchFilterBanner';
import useFeature from 'hooks/useFeature';
import type { SearchBarControl } from 'views/types';

const Container = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
`;

const LeftCol = styled.div`
  flex-grow: 1;
`;

const PluggableSearchBarControls = () => {
  const searchBarControls = usePluginEntities('views.components.searchBar');
  const hasSearchFilterFeature = useFeature('search_filter');
  const leftControls = searchBarControls.filter(({ placement }) => placement === 'left');
  const rightControls = searchBarControls.filter(({ placement }) => placement === 'right');
  const renderControls = (controls: Array<SearchBarControl>) => controls?.map(({ component: ControlComponent, id }) => <ControlComponent key={id} />);
  const hasEnterpriseSearchFilters = searchBarControls.find((control) => control.id === 'search-filters');

  return (
    // eslint-disable-next-line react/jsx-no-useless-fragment
    <Container>
      <LeftCol>
        {hasSearchFilterFeature && (
          <>
            {renderControls(leftControls)}
            {!hasEnterpriseSearchFilters && <SearchFilterBanner />}
          </>
        )}
      </LeftCol>
      <div>{renderControls(rightControls)}</div>
    </Container>
  );
};

export default PluggableSearchBarControls;
