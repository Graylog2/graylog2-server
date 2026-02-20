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
import React, { useCallback } from 'react';
import debounce from 'lodash/debounce';
import styled, { css } from 'styled-components';

import { Input, ListGroupItem } from 'components/bootstrap';
import type { Attribute } from 'stores/PaginationTypes';
import type { Filters } from 'components/common/EntityFilters/types';
import { PaginatedList, NoSearchResult } from 'components/common';
import useIsKeyHeld from 'hooks/useIsKeyHeld';
import Spinner from 'components/common/Spinner';

const Container = styled.div(
  ({ theme }) => css`
    color: ${theme.colors.text.primary};
    padding: 3px 10px;
  `,
);

const SearchInput = styled(Input)`
  margin-bottom: 6px;
`;

const StyledListGroup = styled.div`
  margin-bottom: 0;
`;

const Hint = styled.div(
  ({ theme }) => css`
    margin-top: 9px;
    font-size: ${theme.fonts.size.small};
  `,
);

type SearchParams = {
  query: string;
  page: number;
  pageSize: number;
};

type Suggestion = {
  id: string;
  target_id?: string;
  value: string;
};

type Props = {
  allActiveFilters: Filters | undefined;
  attribute: Attribute;
  filterValueRenderer: (value: unknown, title: string) => React.ReactNode | undefined;
  isLoading: boolean;
  multiSelect: boolean;
  onSubmit: (filter: { title: string; value: string }, closeDropdown: boolean) => void;
  page: number;
  pageSize: number;
  setSearchParams: (updater: (current: SearchParams) => SearchParams) => void;
  suggestions: Array<Suggestion>;
  total: number;
};

const SuggestionsList = ({
  allActiveFilters,
  attribute,
  filterValueRenderer,
  isLoading,
  multiSelect,
  onSubmit,
  page,
  pageSize,
  setSearchParams,
  suggestions,
  total,
}: Props) => {
  const isShiftHeld = useIsKeyHeld('Shift');
  const handleSearchChange = useCallback(
    (newSearchQuery: string) => {
      setSearchParams((cur) => ({ ...cur, page: 1, query: newSearchQuery }));
    },
    [setSearchParams],
  );

  const handlePaginationChange = useCallback(
    (newPage: number) => {
      setSearchParams((cur) => ({ ...cur, page: newPage }));
    },
    [setSearchParams],
  );

  const debounceOnSearch = debounce((value: string) => handleSearchChange(value), 1000);

  return (
    <Container>
      <SearchInput
        type="text"
        id="search-filters-input"
        formGroupClassName=""
        placeholder={`Search for ${attribute.title.toLowerCase()}`}
        onChange={({ target: { value } }) => debounceOnSearch(value)}
      />
      {isLoading && <Spinner />}

      {!!suggestions?.length && (
        <PaginatedList
          showPageSizeSelect={false}
          totalItems={total}
          hidePreviousAndNextPageLinks
          hideFirstAndLastPageLinks
          activePage={page}
          pageSize={pageSize}
          onChange={handlePaginationChange}
          useQueryParameter={false}>
          <StyledListGroup>
            {suggestions.map((suggestion) => {
              const filterValue = suggestion.target_id || suggestion.id;
              const disabled = !!allActiveFilters?.get(attribute.id)?.find(({ value }) => value === filterValue);

              const onClick = () => {
                if (disabled) {
                  return;
                }

                onSubmit(
                  {
                    value: filterValue,
                    title: suggestion.value,
                  },
                  !multiSelect ? true : !isShiftHeld,
                );
              };

              return (
                <ListGroupItem onClick={onClick} key={`filter-value-${suggestion.id}`} disabled={disabled}>
                  {filterValueRenderer ? filterValueRenderer(suggestion.id, suggestion.value) : suggestion.value}
                </ListGroupItem>
              );
            })}
          </StyledListGroup>
        </PaginatedList>
      )}

      {!suggestions?.length && <NoSearchResult>No entities found</NoSearchResult>}

      {multiSelect && (
        <Hint>
          <i>Hold Shift to select multiple</i>
        </Hint>
      )}
    </Container>
  );
};

export default SuggestionsList;
