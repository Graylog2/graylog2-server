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

import IfInteractive from 'views/components/dashboard/IfInteractive';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';

import Pagination from './Pagination';
import PageSizeSelect from './PageSizeSelect';

const DEFAULT_PAGE_SIZES = [10, 50, 100];
export const INITIAL_PAGE = 1;

type Props = {
  activePage?: number,
  children: React.ReactNode,
  className?: string,
  hideFirstAndLastPageLinks?: boolean
  hidePreviousAndNextPageLinks?: boolean
  onChange?: (currentPage: number, pageSize: number) => void,
  pageSizes?: Array<number>,
  pageSize?: number,
  showPageSizeSelect?: boolean,
  totalItems: number,
  useQueryParameter?: boolean,
};

/**
 * Wrapper component around an element that renders pagination
 * controls and provides a callback when the page or page size change.
 * You still need to fetch or filter the data yourself to ensure that
 * the selected page is displayed on screen.
 */
const PaginatedList = ({
  activePage,
  children,
  className,
  hideFirstAndLastPageLinks,
  hidePreviousAndNextPageLinks,
  onChange,
  pageSize: propPageSize,
  pageSizes,
  showPageSizeSelect,
  totalItems,
  useQueryParameter,
}: Props) => {
  const { page, setPage, pageSize: queryParamPageSize, setPageSize } = usePaginationQueryParameter(pageSizes);

  const [{ currentPage, currentPageSize }, setPagination] = React.useState({
    currentPage: useQueryParameter ? page : Math.max(activePage, INITIAL_PAGE),
    currentPageSize: (useQueryParameter && showPageSizeSelect) ? queryParamPageSize : propPageSize,
  });

  const numberPages = React.useMemo(() => (
    currentPageSize > 0 ? Math.ceil(totalItems / currentPageSize) : 0
  ), [currentPageSize, totalItems]);

  const _onChangePageSize = (event: React.ChangeEvent<HTMLOptionElement>) => {
    event.preventDefault();
    const newPageSize = Number(event.target.value);

    setPagination({ currentPage: INITIAL_PAGE, currentPageSize: newPageSize });
    if (useQueryParameter) setPageSize(newPageSize);
    if (onChange) onChange(INITIAL_PAGE, newPageSize);
  };

  const _onChangePage = React.useCallback((pageNum: number) => {
    setPagination({ currentPage: pageNum, currentPageSize });
    if (useQueryParameter) setPage(pageNum);
    if (onChange) onChange(pageNum, currentPageSize);
  }, [useQueryParameter, setPage, onChange, currentPageSize]);

  React.useEffect(() => {
    if (numberPages > 0 && currentPage > numberPages) _onChangePage(numberPages);
  }, [currentPage, numberPages, _onChangePage]);

  return (
    <>
      {showPageSizeSelect && (
        <PageSizeSelect pageSizes={pageSizes} pageSize={currentPageSize} onChange={_onChangePageSize} />
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

PaginatedList.defaultProps = {
  activePage: 1,
  className: undefined,
  hideFirstAndLastPageLinks: false,
  hidePreviousAndNextPageLinks: false,
  pageSizes: DEFAULT_PAGE_SIZES,
  pageSize: DEFAULT_PAGE_SIZES[0],
  showPageSizeSelect: true,
  onChange: undefined,
  useQueryParameter: true,
};

export default PaginatedList;
