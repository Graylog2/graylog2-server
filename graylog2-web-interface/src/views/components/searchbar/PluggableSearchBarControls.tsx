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

import usePluginEntities from 'hooks/usePluginEntities';
import SearchFilterBanner from 'views/components/searchbar/SearchFilterBanner';
import type { SearchBarControl } from 'views/types';
import Store from 'logic/local-storage/Store';
import useFeature from 'hooks/useFeature';

export const PLUGGABLE_CONTROLS_HIDDEN_KEY = 'pluggableSearchBarControlsAreHidden';

const Container = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 10px;
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

const componentHasContent = ({
  hidePluggableControlsPreview,
  showLeftControls,
  showRightControls,
  hasPluggableControls,
  hasSearchFilterFeatureFlag,
  hasLeftColFallback,
  hasRightColFallback,
}:{
  hidePluggableControlsPreview: boolean,
  showLeftControls: boolean,
  showRightControls: boolean,
  hasPluggableControls: boolean,
  hasSearchFilterFeatureFlag: boolean,
  hasLeftColFallback?: boolean,
  hasRightColFallback?: boolean
}) => {
  if (hasPluggableControls) {
    return true;
  }

  if (hidePluggableControlsPreview) {
    return false;
  }

  const shouldShowLeftCol = showLeftControls && hasLeftColFallback && hasSearchFilterFeatureFlag;
  const shouldShowRightCol = showRightControls && !!hasRightColFallback;

  return shouldShowLeftCol || shouldShowRightCol;
};

type Props = {
  showLeftControls?: boolean,
  showRightControls?: boolean,
}

const PluggableSearchBarControls = ({ showLeftControls, showRightControls }: Props) => {
  const [hidePluggableControlsPreview, setHidePluggableControlsPreview] = useState(() => !!Store.get(PLUGGABLE_CONTROLS_HIDDEN_KEY));
  const { leftControls, rightControls } = usePluggableControls();
  const hasSearchFilterFeatureFlag = useFeature('search_filter');
  const hasPluggableControls = !!(leftControls?.length || rightControls?.length);

  const onHidePluggableControlsPreview = useCallback(() => {
    setHidePluggableControlsPreview(true);
    Store.set(PLUGGABLE_CONTROLS_HIDDEN_KEY, true);
  }, []);

  const leftColFallback = <SearchFilterBanner onHide={onHidePluggableControlsPreview} pluggableControls={leftControls} />;

  const shouldRenderContainer = componentHasContent({
    hidePluggableControlsPreview,
    showLeftControls,
    showRightControls,
    hasPluggableControls,
    hasSearchFilterFeatureFlag,
    hasLeftColFallback: !!leftColFallback,
  });

  if (!shouldRenderContainer) return null;

  return (
    <Container>
      <div>
        {hasSearchFilterFeatureFlag && showLeftControls && (
          <>
            {renderControls(leftControls)}
            {leftColFallback}
          </>
        )}
      </div>
      <div>{showRightControls && renderControls(rightControls)}</div>
    </Container>
  );
};

PluggableSearchBarControls.defaultProps = {
  showLeftControls: true,
  showRightControls: true,
};

export default PluggableSearchBarControls;
