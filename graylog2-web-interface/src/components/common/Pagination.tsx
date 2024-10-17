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
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import { Pagination as BootstrapPagination } from 'react-bootstrap';
import { createUltimatePagination, ITEM_TYPES } from 'react-ultimate-pagination';
import styled, { css } from 'styled-components';

import Icon from './Icon';

type Props = {
  currentPage: number,
  totalPages: number,
  boundaryPagesRange?: number,
  siblingPagesRange?: number,
  hideEllipsis?: boolean,
  hidePreviousAndNextPageLinks?: boolean,
  hideFirstAndLastPageLinks?: boolean,
  disabled?: boolean,
  onChange?: (nextPage: number) => void
};

const StyledBootstrapPagination = styled(BootstrapPagination)(({ theme }) => css`
  &.pagination {
    font-size: ${theme.fonts.size.small};
    margin-top: 10px;
    margin-bottom: 0;

    > li {
      > a,
      > span {
        display: flex;
        align-items: center;
        justify-content: center;
        color: ${theme.utils.contrastingColor(theme.colors.global.contentBackground)};
        background-color: ${theme.colors.global.contentBackground};
        border-color: ${theme.colors.variant.light.default};
        border-radius: 0;
        height: 32px;

        &:hover,
        &:focus {
          color: ${theme.utils.contrastingColor(theme.colors.variant.lighter.default)};
          background-color: ${theme.colors.variant.lighter.default};
          border-color: ${theme.colors.variant.light.default};
        }
      }

      &.active > a,
      &.active > span {
        &,
        &:hover,
        &:focus {
          color: ${theme.colors.pagination.active.color};
          background-color: ${theme.colors.pagination.active.background};
          border-color: ${theme.colors.pagination.active.border};
          z-index: 1;
        }
      }

      &.disabled {
        > a,
        > a:hover,
        > a:focus,
        > span,
        > span:hover,
        > span:focus {
          color: ${theme.colors.variant.light.default};
          background-color: ${theme.colors.global.contentBackground};
          border-color: ${theme.colors.variant.lighter.default};
        }
      }
      
    }
  }
`);

const UltimatePagination = createUltimatePagination({
  WrapperComponent: StyledBootstrapPagination,
  itemTypeToComponent: {

    [ITEM_TYPES.PAGE]: ({ value, isActive, onClick }) => {
      const title = isActive ? 'Active page' : `Open page ${value}`;

      return (
        <BootstrapPagination.Item active={isActive}
                                  onClick={onClick}
                                  title={title}
                                  aria-label={title}>
          {value}
        </BootstrapPagination.Item>
      );
    },
    [ITEM_TYPES.ELLIPSIS]: ({ isActive, onClick }) => {
      const title = 'Open following page';

      return (
        <BootstrapPagination.Ellipsis disabled={isActive}
                                      onClick={onClick}
                                      title={title}
                                      aria-label={title}
                                      className="pagination-control">
          <Icon name="more_horiz" />
        </BootstrapPagination.Ellipsis>
      );
    },
    [ITEM_TYPES.FIRST_PAGE_LINK]: ({ isActive, onClick }) => {
      const title = 'Open first page';

      return (
        <BootstrapPagination.First disabled={isActive}
                                   onClick={onClick}
                                   title={title}
                                   aria-label={title}
                                   className="pagination-control">
          <Icon name="keyboard_double_arrow_left" />
        </BootstrapPagination.First>
      );
    },
    [ITEM_TYPES.PREVIOUS_PAGE_LINK]: ({ isActive, onClick }) => {
      const title = 'Open previous page';

      return (
        <BootstrapPagination.Prev disabled={isActive}
                                  onClick={onClick}
                                  title={title}
                                  aria-label={title}
                                  className="pagination-control">
          <Icon name="chevron_left" />
        </BootstrapPagination.Prev>
      );
    },
    [ITEM_TYPES.NEXT_PAGE_LINK]: ({ isActive, onClick }) => {
      const title = 'Open next page';

      return (
        <BootstrapPagination.Next disabled={isActive}
                                  onClick={onClick}
                                  title={title}
                                  aria-label={title}
                                  className="pagination-control">
          <Icon name="chevron_right" />
        </BootstrapPagination.Next>
      );
    },
    [ITEM_TYPES.LAST_PAGE_LINK]: ({ isActive, onClick }) => {
      const title = 'Open last page';

      return (
        <BootstrapPagination.Last disabled={isActive}
                                  onClick={onClick}
                                  title={title}
                                  aria-label={title}
                                  className="pagination-control">
          <Icon name="keyboard_double_arrow_right" />
        </BootstrapPagination.Last>
      );
    },

  },
});

const Pagination = ({
  currentPage,
  totalPages,
  boundaryPagesRange = 1,
  siblingPagesRange = 1,
  hideEllipsis = false,
  hidePreviousAndNextPageLinks = false,
  hideFirstAndLastPageLinks = false,
  disabled = false,
  onChange = () => {},
}: Props) => {
  if (totalPages <= 1) {
    return null;
  }

  if (currentPage > totalPages) {
    // eslint-disable-next-line no-console
    console.warn('Graylog Pagination: `currentPage` prop should not be larger than `totalPages` prop.');

    return null;
  }

  return (
    <UltimatePagination currentPage={currentPage}
                        totalPages={totalPages}
                        boundaryPagesRange={boundaryPagesRange}
                        siblingPagesRange={siblingPagesRange}
                        hideEllipsis={hideEllipsis}
                        hidePreviousAndNextPageLinks={hidePreviousAndNextPageLinks}
                        hideFirstAndLastPageLinks={hideFirstAndLastPageLinks}
                        disabled={disabled}
                        onChange={onChange}
                        data-testid="graylog-pagination" />
  );
};

export default Pagination;
