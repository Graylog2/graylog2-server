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
import { useState, useCallback } from 'react';

import usePluginEntities from 'views/logic/usePluginEntities';
import SearchFilterBanner from 'views/components/searchbar/SearchFilterBanner';
import type { SearchBarControl } from 'views/types';
import Store from 'logic/local-storage/Store';
import useFeature from 'hooks/useFeature';

export const PLUGGABLE_CONTROLS_HIDDEN_KEY = 'pluggableSearchBarControlsAreHidden';

const Container = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
`;

const usePluggableControls = () => {
  const searchBarControls = usePluginEntities('views.components.searchBar') ?? [];
  const existingControls = searchBarControls.map((controlFn) => controlFn()).filter((control) => !!control);
  const leftControls = existingControls.filter(({ placement }) => placement === 'left');
  const rightControls = existingControls.filter(({ placement }) => placement === 'right');

  return ({
    leftControls,
    rightControls,
  });
};

const renderControls = (controls: Array<SearchBarControl>) => controls?.map(({ component: ControlComponent, id }) => <ControlComponent key={id} />);

const PluggableSearchBarControls = () => {
  const [hidePluggableControlsPreview, setHidePluggableControlsPreview] = useState(!!Store.get(PLUGGABLE_CONTROLS_HIDDEN_KEY));
  const { leftControls, rightControls } = usePluggableControls();
  const hasSearchFilterFeatureFlag = useFeature('search_filter');
  const hasPluggableControls = !!(leftControls?.length || rightControls?.length);
  const shouldRenderContainer = (hasPluggableControls || (!hidePluggableControlsPreview && hasSearchFilterFeatureFlag));

  const onHidePluggableControlsPreview = useCallback(() => {
    setHidePluggableControlsPreview(true);
    Store.set(PLUGGABLE_CONTROLS_HIDDEN_KEY, true);
  }, []);

  if (!shouldRenderContainer) return null;

  return (
    <Container>
      <div>
        {hasSearchFilterFeatureFlag && (
          <>
            {renderControls(leftControls)}
            <SearchFilterBanner onHide={onHidePluggableControlsPreview} pluggableControls={leftControls} />
          </>
        )}
      </div>
      <div>{renderControls(rightControls)}</div>
    </Container>
  );
};

export default PluggableSearchBarControls;
