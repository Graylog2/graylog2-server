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
import React, { useState, useCallback } from 'react';
import debounce from 'lodash/debounce';
import styled, { css } from 'styled-components';
import { useQuery } from '@tanstack/react-query';

import { Input, ListGroupItem } from 'components/bootstrap';
import type { Attribute } from 'stores/PaginationTypes';
import type { Filter, Filters } from 'components/common/EntityFilters/types';
import { PaginatedList } from 'components/common';
import generateId from 'logic/generateId';
import UserNotification from 'util/UserNotification';
import PaginationURL from 'util/PaginationURL';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import useIsKeyHeld from 'hooks/useIsKeyHeld';

import Spinner from '../../Spinner';

export type PaginatedFilterValueSuggestions = {
  suggestions?: Array<{ id: string, value: string }>,
  pagination: {
    page: number,
    per_page: number,
    count: number,
    total: number,
  }
}

export type RequestQuery = {
  page: number,
  per_page: number,
  query?: string,
  sort?: string,
  order?: string,
  scope?: string
};

export const DEFAULT_PAGINATION = {
  per_page: 25,
  page: 1,
  count: 0,
  total: 0,
};

const DEFAULT_QUERY = {
  query: '',
  per_page: 10,
  page: 1,
  sort: 'title',
  order: 'asc',
  scope: 'ALL',
};

const Container = styled.div(({ theme }) => css`
  color: ${theme.colors.global.textDefault};
  padding: 3px 10px;
`);

const SearchInput = styled(Input)`
  margin-bottom: 6px;
`;

const StyledListGroup = styled.div`
  margin-bottom: 0;
`;

const Hint = styled.div(({ theme }) => css`
  margin-top: 9px;
  font-size: ${theme.fonts.size.small};
`);

const fetchFilterValueSuggestions = async (collection: string, { query, page, per_page }: RequestQuery): Promise<PaginatedFilterValueSuggestions> => {
  const additional = {
    collection,
    column: 'title',
  };
  const url = PaginationURL('entity_suggestions', page, per_page, query, additional);

  return fetch('GET', qualifyUrl(url));
};

const useFilterValueSuggestions = (attributeId: string, collection: string, searchParams: RequestQuery):{ data: PaginatedFilterValueSuggestions, isFetching: boolean } => {
  if (!collection) {
    throw Error(`Attribute meta data for attribute "${attributeId}" is missing related collection.`);
  }

  return useQuery(['filters', 'suggestions', searchParams], () => fetchFilterValueSuggestions(collection, searchParams), {
    onError: (errorThrown) => {
      UserNotification.error(`Loading suggestions for filter failed with status: ${errorThrown}`,
        'Could not load filter suggestions');
    },
    retry: 0,
    keepPreviousData: true,
    initialData: {
      pagination: DEFAULT_PAGINATION,
    },
  });
};

type Props = {
  attribute: Attribute,
  filterValueRenderer: (value: unknown, title: string) => React.ReactNode | undefined,
  onSubmit: (filter: Filter, closeDropdown: boolean) => void,
  allActiveFilters: Filters | undefined,
  scenario: 'create' | 'edit'
}

const SuggestionsList = ({ attribute, filterValueRenderer, onSubmit, allActiveFilters, scenario }: Props) => {
  const isShiftHeld = useIsKeyHeld('Shift');
  const [searchParams, setSearchParams] = useState(DEFAULT_QUERY);
  const { data: { pagination, suggestions }, isFetching } = useFilterValueSuggestions(attribute.id, attribute.related_collection, searchParams);
  const handleSearchChange = useCallback((newSearchQuery: string) => {
    setSearchParams((cur) => ({ ...cur, page: DEFAULT_QUERY.page, query: newSearchQuery }));
  }, [setSearchParams]);

  const handlePaginationChange = useCallback((page: number) => {
    setSearchParams((cur) => ({ ...cur, page }));
  }, []);

  const debounceOnSearch = debounce((value: string) => handleSearchChange(value), 1000);

  return (
    <Container>
      <SearchInput type="text"
                   id="search-filters-input"
                   formGroupClassName=""
                   placeholder="Search for entity"
                   onChange={({ target: { value } }) => debounceOnSearch(value)} />
      {(!suggestions?.length && isFetching) && <Spinner />}

      {!!suggestions?.length && (
        <PaginatedList showPageSizeSelect={false}
                       totalItems={pagination.total}
                       hidePreviousAndNextPageLinks
                       hideFirstAndLastPageLinks
                       activePage={searchParams.page}
                       pageSize={searchParams.per_page}
                       onChange={handlePaginationChange}
                       useQueryParameter={false}>
          <StyledListGroup>
            {suggestions.map((suggestion) => (
              <ListGroupItem onClick={() => onSubmit({ value: suggestion.id, title: suggestion.value, id: generateId() }, !isShiftHeld)}
                             key={`filter-value-${suggestion.id}`}
                             disabled={!!allActiveFilters?.[attribute.id]?.find(({ value }) => value === suggestion.id)}>
                {filterValueRenderer ? filterValueRenderer(suggestion.id, suggestion.value) : suggestion.value}
              </ListGroupItem>
            ))}
          </StyledListGroup>
        </PaginatedList>
      )}
      {scenario === 'create' && (
        <Hint>
          <i>
            Hold Shift to select multiple
          </i>
        </Hint>
      )}
    </Container>
  );
};

export default SuggestionsList;
