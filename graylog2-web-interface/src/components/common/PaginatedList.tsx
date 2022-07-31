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
import PropTypes from 'prop-types';
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
  const currentPage = useQueryParameter ? page : Math.max(activePage, INITIAL_PAGE);
  const pageSize = (useQueryParameter && showPageSizeSelect) ? queryParamPageSize : propPageSize;

  const numberPages = pageSize > 0 ? Math.ceil(totalItems / pageSize) : 0;

  const _onChangePageSize = (event: React.ChangeEvent<HTMLOptionElement>) => {
    event.preventDefault();
    const newPageSize = Number(event.target.value);

    if (useQueryParameter) setPageSize(newPageSize);
    if (onChange) onChange(INITIAL_PAGE, newPageSize);
  };

  const _onChangePage = (pageNum: number) => {
    if (useQueryParameter) setPage(pageNum);
    if (onChange) onChange(pageNum, pageSize);
  };

  return (
    <>
      {showPageSizeSelect && (
        <PageSizeSelect pageSizes={pageSizes} pageSize={pageSize} onChange={_onChangePageSize} />
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

PaginatedList.propTypes = {
  /** The active page number. If not specified the active page number will be tracked internally. */
  activePage: PropTypes.number,
  /** React element containing items of the current selected page. */
  children: PropTypes.node.isRequired,
  /**
   * Function that will be called when the page changes.
   * It receives the current page and the page size as arguments.
   */
  onChange: PropTypes.func,
  /** boolean flag to hide first and last page links */
  hideFirstAndLastPageLinks: PropTypes.bool,
  /**  boolean flag to hide previous and next page links */
  hidePreviousAndNextPageLinks: PropTypes.bool,
  /** Array of different items per page that are allowed. */
  pageSizes: PropTypes.arrayOf(PropTypes.number),
  /** Number of items per page. */
  pageSize: PropTypes.number,
  /** Whether to show the page size selector or not. */
  showPageSizeSelect: PropTypes.bool,
  /** Total amount of items in all pages. */
  totalItems: PropTypes.number.isRequired,
  /** boolean flag to see if we should save and use page and pageSize as query parameters */
  useQueryParameter: PropTypes.bool,
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
