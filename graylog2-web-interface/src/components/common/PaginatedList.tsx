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
import { useEffect, useMemo, useCallback, useState } from 'react';

import IfInteractive from 'views/components/dashboard/IfInteractive';
import usePaginationQueryParameter, { DEFAULT_PAGE_SIZES } from 'hooks/usePaginationQueryParameter';

import Pagination from './Pagination';
import PageSizeSelect from './PageSizeSelect';

export const INITIAL_PAGE = 1;

type Props = {
  children: React.ReactNode,
  className?: string,
  hideFirstAndLastPageLinks?: boolean
  hidePreviousAndNextPageLinks?: boolean
  onChange?: (currentPage: number, pageSize: number) => void,
  pageSizes?: Array<number>,
  showPageSizeSelect?: boolean,
  totalItems: number,
};

const ListBase = ({
  children,
  className,
  currentPage,
  currentPageSize,
  hideFirstAndLastPageLinks,
  hidePreviousAndNextPageLinks,
  onChange,
  pageSizes,
  setPagination,
  showPageSizeSelect,
  totalItems,
}: Required<Props> & {
  currentPageSize: number,
  currentPage: number;
  setPagination: (newPagination: { page: number, pageSize: number }) => void
}) => {
  const numberPages = useMemo(() => (
    currentPageSize > 0 ? Math.ceil(totalItems / currentPageSize) : 0
  ), [currentPageSize, totalItems]);

  const _onChangePageSize = useCallback((newPageSize: number) => {
    setPagination({ page: INITIAL_PAGE, pageSize: newPageSize });
    if (onChange) onChange(INITIAL_PAGE, newPageSize);
  }, [onChange, setPagination]);

  const _onChangePage = useCallback((pageNum: number) => {
    setPagination({ page: pageNum, pageSize: currentPageSize });
    if (onChange) onChange(pageNum, currentPageSize);
  }, [setPagination, currentPageSize, onChange]);

  useEffect(() => {
    if (numberPages > 0 && currentPage > numberPages) _onChangePage(numberPages);
  }, [currentPage, numberPages, _onChangePage]);

  return (
    <>
      {showPageSizeSelect && (
        <PageSizeSelect pageSizes={pageSizes}
                        pageSize={currentPageSize}
                        showLabel
                        onChange={_onChangePageSize}
                        className="pull-right" />
      )}

      {children}

      <IfInteractive>
        <div className={`text-center pagination-wrapper ${className ?? ''}`}>
          <Pagination totalPages={numberPages}
                      currentPage={currentPage}
                      hidePreviousAndNextPageLinks={hidePreviousAndNextPageLinks}
                      hideFirstAndLastPageLinks={hideFirstAndLastPageLinks}
                      onChange={_onChangePage} />
        </div>
      </IfInteractive>
    </>
  );
};

const ListBasedOnQueryParams = ({
  pageSizes,
  ...props
}: Required<Props> & { pageSize: number }) => {
  const { page: currentPage, pageSize: currentPageSize, setPagination } = usePaginationQueryParameter(pageSizes, props.pageSize, props.showPageSizeSelect);

  return <ListBase {...props} currentPage={currentPage} currentPageSize={currentPageSize} setPagination={setPagination} pageSizes={pageSizes} />;
};

const ListWithOwnState = ({
  activePage,
  pageSize: propPageSize,
  ...props
}: Required<Props> & { activePage: number, pageSize: number }) => {
  const [currentPage, setCurrentPage] = useState<number>(Math.max(activePage, INITIAL_PAGE));
  const [currentPageSize, setCurrentPageSize] = useState<number>(propPageSize);

  useEffect(() => {
    if (activePage > 0) {
      setCurrentPage(activePage);
    }
  }, [activePage]);

  useEffect(() => {
    setCurrentPageSize(propPageSize);
  }, [propPageSize]);

  const setPagination = useCallback(({ page, pageSize }) => {
    setCurrentPageSize(pageSize);
    setCurrentPage(page);
  }, []);

  return (
    <ListBase {...props}
              currentPage={currentPage}
              currentPageSize={currentPageSize}
              setPagination={setPagination} />
  );
};

/**
 * Wrapper component around an element that renders pagination
 * controls and provides a callback when the page or page size change.
 * You still need to fetch or filter the data yourself to ensure that
 * the selected page is displayed on screen.
 */
const PaginatedList = ({
  activePage = 1,
  children,
  className,
  hideFirstAndLastPageLinks = false,
  hidePreviousAndNextPageLinks = false,
  onChange,
  pageSize = DEFAULT_PAGE_SIZES[0],
  pageSizes = DEFAULT_PAGE_SIZES,
  showPageSizeSelect = true,
  totalItems,
  useQueryParameter = true,
}: Props & {
  activePage?: number,
  pageSize?: number,
  useQueryParameter?: boolean,
}) => {
  if (useQueryParameter) {
    return (
      <ListBasedOnQueryParams className={className}
                              hideFirstAndLastPageLinks={hideFirstAndLastPageLinks}
                              hidePreviousAndNextPageLinks={hidePreviousAndNextPageLinks}
                              onChange={onChange}
                              pageSizes={pageSizes}
                              pageSize={pageSize}
                              showPageSizeSelect={showPageSizeSelect}
                              totalItems={totalItems}>
        {children}
      </ListBasedOnQueryParams>
    );
  }

  return (
    <ListWithOwnState className={className}
                      hideFirstAndLastPageLinks={hideFirstAndLastPageLinks}
                      hidePreviousAndNextPageLinks={hidePreviousAndNextPageLinks}
                      onChange={onChange}
                      pageSizes={pageSizes}
                      pageSize={pageSize}
                      showPageSizeSelect={showPageSizeSelect}
                      totalItems={totalItems}
                      activePage={activePage}>
      {children}
    </ListWithOwnState>
  );
};

export default PaginatedList;
