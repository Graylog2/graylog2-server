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
import React, { useCallback, useMemo } from 'react';
import { useQueryParam, StringParam } from 'use-query-params';

import {
  NoEntitiesExist,
  PaginatedList, SearchForm,
  Spinner,
} from 'components/common';
import EntityDataTable from 'components/common/EntityDataTable';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import type { Sort } from 'stores/PaginationTypes';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import EntityFilters from 'components/common/EntityFilters';
import useUrlQueryFilters from 'components/common/EntityFilters/hooks/useUrlQueryFilters';
import type { UrlQueryFilters } from 'components/common/EntityFilters/types';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import type { IndexSetTemplate }
  from 'components/indices/IndexSetTemplates/types';
import useTemplates
  from 'components/indices/IndexSetTemplates/hooks/useTemplates';
import TemplateActions from 'components/indices/IndexSetTemplates/TemplateActions';

export const ENTITY_TABLE_ID = 'index-set-template';
export const DEFAULT_LAYOUT = {
  pageSize: 20,
  sort: { attributeId: 'title', direction: 'asc' } as Sort,
  displayedColumns: ['title', 'description'],
  columnsOrder: ['title', 'description'],
};

const IndexSetTemplatesList = () => {
  const [urlQueryFilters, setUrlQueryFilters] = useUrlQueryFilters();
  const [query, setQuery] = useQueryParam('query', StringParam);
  const { layoutConfig, isInitialLoading: isLoadingLayoutPreferences } = useTableLayout({
    entityTableId: ENTITY_TABLE_ID,
    defaultPageSize: DEFAULT_LAYOUT.pageSize,
    defaultDisplayedAttributes: DEFAULT_LAYOUT.displayedColumns,
    defaultSort: DEFAULT_LAYOUT.sort,
  });
  const paginationQueryParameter = usePaginationQueryParameter(undefined, layoutConfig.pageSize, false);
  const searchParams = useMemo(() => ({
    query,
    page: paginationQueryParameter.page,
    pageSize: layoutConfig.pageSize,
    sort: layoutConfig.sort,
    filters: urlQueryFilters,
  }), [paginationQueryParameter.page, layoutConfig.pageSize, layoutConfig.sort, query, urlQueryFilters]);
  const { mutate: updateTableLayout } = useUpdateUserLayoutPreferences(ENTITY_TABLE_ID);

  const onPageSizeChange = useCallback((newPageSize: number) => {
    paginationQueryParameter.resetPage();
    updateTableLayout({ perPage: newPageSize });
  }, [paginationQueryParameter, updateTableLayout]);

  const onSortChange = useCallback((newSort: Sort) => {
    paginationQueryParameter.resetPage();
    updateTableLayout({ sort: newSort });
  }, [paginationQueryParameter, updateTableLayout]);

  const onColumnsChange = useCallback((displayedAttributes: Array<string>) => {
    updateTableLayout({ displayedAttributes });
  }, [updateTableLayout]);

  const {
    isLoading,
    data: { list, pagination, attributes },
  } = useTemplates(
    searchParams,
    { enabled: !isLoadingLayoutPreferences },
  );

  const onSearch = useCallback((val: string) => {
    paginationQueryParameter.resetPage();
    setQuery(val);
  }, [paginationQueryParameter, setQuery]);

  const onSearchReset = useCallback(() => setQuery(''), [setQuery]);

  const onChangeFilters = useCallback((newUrlQueryFilters: UrlQueryFilters) => {
    paginationQueryParameter.resetPage();
    setUrlQueryFilters(newUrlQueryFilters);
  }, [paginationQueryParameter, setUrlQueryFilters]);

  const templateActions = useCallback(({ id, title }: IndexSetTemplate) => <TemplateActions templateId={id} templateTitle={title} />, []);

  if (isLoadingLayoutPreferences || isLoading) {
    return <Spinner />;
  }

  return (
    <PaginatedList totalItems={pagination?.total}
                   pageSize={layoutConfig.pageSize}
                   showPageSizeSelect={false}>
      <div style={{ marginBottom: 5 }}>
        <SearchForm onSearch={onSearch}
                    onReset={onSearchReset}
                    query={query}
                    placeholder="Search for index set template...">
          <EntityFilters attributes={attributes}
                         urlQueryFilters={urlQueryFilters}
                         setUrlQueryFilters={onChangeFilters} />
        </SearchForm>
      </div>
      {pagination?.total === 0 && (
        <NoEntitiesExist>
          No index set templates have been found.
        </NoEntitiesExist>
      )}
      {!!list?.length && (
        <EntityDataTable<IndexSetTemplate> data={list}
                                           visibleColumns={layoutConfig.displayedAttributes}
                                           columnsOrder={DEFAULT_LAYOUT.columnsOrder}
                                           onColumnsChange={onColumnsChange}
                                           onSortChange={onSortChange}
                                           activeSort={layoutConfig.sort}
                                           pageSize={searchParams.pageSize}
                                           onPageSizeChange={onPageSizeChange}
                                           actionsCellWidth={120}
                                           columnDefinitions={attributes}
                                           rowActions={templateActions} />
      )}
    </PaginatedList>
  );
};

export default IndexSetTemplatesList;
