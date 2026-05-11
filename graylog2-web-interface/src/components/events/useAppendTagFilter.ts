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
import useUrlQueryFilters from 'components/common/EntityFilters/hooks/useUrlQueryFilters';
import useTableFetchContext from 'components/common/PaginatedEntityTable/useTableFetchContext';

const useAppendTagFilter = () => {
  const { searchParams } = useTableFetchContext();
  const [, setUrlFilters] = useUrlQueryFilters();

  return (tag: string) => {
    const currentFilters = searchParams.filters;
    const existing = currentFilters.get('tags', []);
    if (existing.includes(tag)) return;
    setUrlFilters(currentFilters.set('tags', [...existing, tag]));
  };
};

export default useAppendTagFilter;
