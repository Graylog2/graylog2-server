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

import { Button, ButtonGroup } from 'components/bootstrap';
import { Icon, HoverForHelp } from 'components/common';
import type { SearchBarControl } from 'views/types';

const Container = styled.div`
  display: flex;
  align-items: center;
`;

const StyledButtonGroup = styled(ButtonGroup)`
  margin-left: 5px;
`;

const StyledButtonBar = styled.div`
  display: flex;
  justify-content: flex-end;
`;

const SearchFilterHelp = styled(HoverForHelp)`
  margin-left: 5px;
`;

export const SearchFilterExplanation = () => (
  <>
    <p>
      <i>Search filters</i> contain their own query and extend the main query using the <b>AND</b> operator.
    </p>
    <p>
      Filters can be saved separately and reused in saved searches and dashboards.
      Updating a saved filter will automatically affect the search results of searches which include the filter.
    </p>
  </>
);

type Props = {
  onHide: () => void,
  pluggableControls: Array<SearchBarControl>
}

const SearchFilterBanner = ({ onHide, pluggableControls }: Props) => {
  const hasSearchFiltersPlugin = !!pluggableControls.find((control) => control.id === 'search-filters');

  if (hasSearchFiltersPlugin) {
    return null;
  }

  return (
    <Container>
      Filters
      <SearchFilterHelp title="Search Filters" trigger={['click']}>
        <SearchFilterExplanation />
        <p>
          Search filters and parameters are available for the enterprise version.
        </p>
        <StyledButtonBar>
          <Button onClick={onHide} bsSize="xs">
            Hide controls
          </Button>
        </StyledButtonBar>
      </SearchFilterHelp>
      <StyledButtonGroup>
        <Button disabled bsSize="small">
          <Icon name="plus" />
        </Button>
        <Button disabled bsSize="small">
          <Icon name="folder" />
        </Button>
      </StyledButtonGroup>
    </Container>
  );
};

export default SearchFilterBanner;
