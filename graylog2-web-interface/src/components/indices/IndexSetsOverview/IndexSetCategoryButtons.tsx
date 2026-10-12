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
import { OrderedMap } from 'immutable';
import styled, { css } from 'styled-components';

import { AccessibleCard } from 'components/common';
import { useTableFilterContext } from 'components/common/PaginatedEntityTable';
import type { UrlQueryFilters } from 'components/common/EntityFilters/types';

import { CATEGORY_ATTRIBUTE, INDEX_SET_CATEGORY_TITLES } from './Constants';
import useIndexSetCategoryCounts from './hooks/useIndexSetCategoryCounts';
import type { IndexSetCategory } from './types';

const ALL = 'all';

type CategoryOption = typeof ALL | IndexSetCategory;

const CATEGORY_OPTIONS: Array<{ value: CategoryOption; title: string }> = [
  { value: ALL, title: 'All' },
  { value: 'user', title: INDEX_SET_CATEGORY_TITLES.user },
  { value: 'illuminate', title: INDEX_SET_CATEGORY_TITLES.illuminate },
  { value: 'system', title: INDEX_SET_CATEGORY_TITLES.system },
];

const Container = styled.div<{ $columns: number }>(
  ({ theme, $columns }) => css`
    display: grid;
    grid-template-columns: repeat(${$columns}, minmax(0, 1fr));
    gap: ${theme.spacings.sm};
    width: 100%;
    margin-bottom: ${theme.spacings.sm};
  `,
);

const CategoryCard = styled(AccessibleCard)(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xs};
    width: 100%;
    color: inherit;
    font: inherit;
    text-align: left;
  `,
);

const activeCategory = (filters: UrlQueryFilters | undefined): CategoryOption | undefined => {
  const selected = filters?.get(CATEGORY_ATTRIBUTE) ?? [];

  if (selected.length === 0) {
    return ALL;
  }

  return selected.length === 1 ? (selected[0] as CategoryOption) : undefined;
};

const IndexSetCategoryButtons = () => {
  const { searchParams, onChangeFilters } = useTableFilterContext();
  const { data: counts } = useIndexSetCategoryCounts();
  const active = activeCategory(searchParams.filters);

  const onSelect = (category: CategoryOption) => {
    const filters = searchParams.filters ?? OrderedMap<string, Array<string>>();

    onChangeFilters(
      category === ALL ? filters.delete(CATEGORY_ATTRIBUTE) : filters.set(CATEGORY_ATTRIBUTE, [category]),
    );
  };

  const visibleOptions = CATEGORY_OPTIONS.filter(
    ({ value }) => value !== 'illuminate' || active === 'illuminate' || (counts?.illuminate ?? 0) > 0,
  );

  return (
    <Container $columns={visibleOptions.length}>
      {visibleOptions.map(({ value, title }) => (
        <CategoryCard key={value} isActive={active === value} onClick={() => onSelect(value)}>
          <h3>{title}</h3>
          <h2>
            <b>{counts?.[value] ?? '–'}</b>
          </h2>
        </CategoryCard>
      ))}
    </Container>
  );
};

export default IndexSetCategoryButtons;
