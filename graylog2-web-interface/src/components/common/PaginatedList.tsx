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
import { useEffect } from 'react';

import IfInteractive from 'views/components/dashboard/IfInteractive';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import useIsMountedRef from 'hooks/useIsMountedRef';

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
  onChange: (currentPage: number, pageSize: number) => void,
  pageSize?: number,
  pageSizes?: Array<number>,
  showPageSizeSelect?: boolean,
  totalItems: number,
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
  pageSize: propsPageSize,
  pageSizes,
  showPageSizeSelect,
  totalItems,
}: Props) => {
  const isMounted = useIsMountedRef();

  const { page, setPage, pageSize, setPageSize } = usePaginationQueryParameter(pageSizes);
  const currentPage = page > 0 ? page : INITIAL_PAGE;
  const numberPages = pageSize > 0 ? Math.ceil(totalItems / pageSize) : 0;

  useEffect(() => {
    if (isMounted.current && ((currentPage !== activePage) || (pageSize !== propsPageSize))) {
      onChange(currentPage, pageSize);
      isMounted.current = false;
    }
  }, [isMounted, currentPage, pageSize, activePage, propsPageSize, onChange]);

  const _onChangePageSize = (event: React.ChangeEvent<HTMLOptionElement>) => {
    event.preventDefault();
    const newPageSize = Number(event.target.value);

    setPageSize(newPageSize);
    onChange(INITIAL_PAGE, newPageSize);
  };

  const _onChangePage = (pageNum: number) => {
    setPage(pageNum);
    onChange(pageNum, pageSize);
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
  onChange: PropTypes.func.isRequired,
  /** boolean flag to hide first and last page links */
  hideFirstAndLastPageLinks: PropTypes.bool,
  /**  boolean flag to hide previous and next page links */
  hidePreviousAndNextPageLinks: PropTypes.bool,
  /** Number of items per page. */
  pageSize: PropTypes.number,
  /** Array of different items per page that are allowed. */
  pageSizes: PropTypes.arrayOf(PropTypes.number),
  /** Whether to show the page size selector or not. */
  showPageSizeSelect: PropTypes.bool,
  /** Total amount of items in all pages. */
  totalItems: PropTypes.number.isRequired,
};

PaginatedList.defaultProps = {
  activePage: 1,
  className: undefined,
  hideFirstAndLastPageLinks: false,
  hidePreviousAndNextPageLinks: false,
  pageSize: DEFAULT_PAGE_SIZES[0],
  pageSizes: DEFAULT_PAGE_SIZES,
  showPageSizeSelect: true,
};

export default PaginatedList;
