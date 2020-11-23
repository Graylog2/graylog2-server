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
import { useState, useEffect } from 'react';

import IfInteractive from 'views/components/dashboard/IfInteractive';

import Pagination from './Pagination';
import PageSizeSelect from './PageSizeSelect';

const DEFAULT_PAGE_SIZES = [10, 50, 100];
export const INITIAL_PAGE = 1;

type Props = {
  children: React.ReactNode,
  className?: string,
  onChange: (currentPage: number, pageSize: number) => void,
  activePage: number,
  pageSize: number,
  pageSizes: Array<number>,
  totalItems: number,
  showPageSizeSelect: boolean,
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
  onChange,
  pageSize: propsPageSize,
  pageSizes,
  showPageSizeSelect,
  totalItems,
}: Props) => {
  const initialPage = activePage > 0 ? activePage : INITIAL_PAGE;
  const [pageSize, setPageSize] = useState(propsPageSize);
  const [currentPage, setCurrentPage] = useState(initialPage);
  const numberPages = pageSize > 0 ? Math.ceil(totalItems / pageSize) : 0;

  useEffect(() => {
    if (activePage > 0) {
      setCurrentPage(activePage);
    }
  }, [activePage]);

  useEffect(() => {
    setPageSize(propsPageSize);
  }, [propsPageSize]);

  const _onChangePageSize = (event: React.ChangeEvent<HTMLOptionElement>) => {
    event.preventDefault();
    const newPageSize = Number(event.target.value);

    setCurrentPage(INITIAL_PAGE);
    setPageSize(newPageSize);
    onChange(INITIAL_PAGE, newPageSize);
  };

  const _onChangePage = (pageNum: number) => {
    setCurrentPage(pageNum);
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
                      onChange={_onChangePage} />
        </div>
      </IfInteractive>
    </>
  );
};

PaginatedList.propTypes = {
  /** React element containing items of the current selected page. */
  children: PropTypes.node.isRequired,
  /**
   * Function that will be called when the page changes.
   * It receives the current page and the page size as arguments.
   */
  onChange: PropTypes.func.isRequired,
  /** The active page number. If not specified the active page number will be tracked internally. */
  activePage: PropTypes.number,
  /** Number of items per page. */
  pageSize: PropTypes.number,
  /** Array of different items per page that are allowed. */
  pageSizes: PropTypes.arrayOf(PropTypes.number),
  /** Total amount of items in all pages. */
  totalItems: PropTypes.number.isRequired,
  /** Whether to show the page size selector or not. */
  showPageSizeSelect: PropTypes.bool,
};

PaginatedList.defaultProps = {
  activePage: 0,
  className: undefined,
  pageSizes: DEFAULT_PAGE_SIZES,
  pageSize: DEFAULT_PAGE_SIZES[0],
  showPageSizeSelect: true,
};

export default PaginatedList;
