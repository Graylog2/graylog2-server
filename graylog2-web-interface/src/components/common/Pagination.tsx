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
import PropTypes from 'prop-types';
// eslint-disable-next-line no-restricted-imports
import { Pagination as BootstrapPagination } from 'react-bootstrap';
import { createUltimatePagination, ITEM_TYPES } from 'react-ultimate-pagination';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';

import Icon from './Icon';

type Props = {
  currentPage: number,
  totalPages: number,
  boundaryPagesRange: number,
  siblingPagesRange: number,
  hideEllipsis: boolean,
  hidePreviousAndNextPageLinks: boolean,
  hideFirstAndLastPageLinks: boolean,
  disabled: boolean,
  onChange: (nextPage: number) => void,
};

const StyledBootstrapPagination: StyledComponent<undefined, ThemeInterface, unknown> = styled(BootstrapPagination)(({ theme }) => css`
  &.pagination {
    font-size: ${theme.fonts.size.small};
    margin-top: 10px;
    margin-bottom: 0;

    > li {
      > a,
      > span {
        color: ${theme.utils.contrastingColor(theme.colors.global.contentBackground)};
        background-color: ${theme.colors.global.contentBackground};
        border-color: ${theme.colors.variant.light.default};

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
          color: ${theme.utils.contrastingColor(theme.colors.variant.lightest.info)};
          background-color: ${theme.colors.variant.lightest.info};
          border-color: ${theme.colors.variant.lighter.info};
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
    /* eslint-disable react/prop-types */
    [ITEM_TYPES.PAGE]: ({ value, isActive, onClick }) => (
      <BootstrapPagination.Item active={isActive} onClick={onClick}>
        {value}
      </BootstrapPagination.Item>
    ),
    [ITEM_TYPES.ELLIPSIS]: ({ isActive, onClick }) => (
      <BootstrapPagination.Ellipsis disabled={isActive} onClick={onClick}>
        <Icon name="ellipsis-h" />
      </BootstrapPagination.Ellipsis>
    ),
    [ITEM_TYPES.FIRST_PAGE_LINK]: ({ isActive, onClick }) => (
      <BootstrapPagination.First disabled={isActive} onClick={onClick}>
        <Icon name="angle-double-left" />
      </BootstrapPagination.First>
    ),
    [ITEM_TYPES.PREVIOUS_PAGE_LINK]: ({ isActive, onClick }) => (
      <BootstrapPagination.Prev disabled={isActive} onClick={onClick}>
        <Icon name="angle-left" />
      </BootstrapPagination.Prev>
    ),
    [ITEM_TYPES.NEXT_PAGE_LINK]: ({ isActive, onClick }) => (
      <BootstrapPagination.Next disabled={isActive} onClick={onClick}>
        <Icon name="angle-right" />
      </BootstrapPagination.Next>
    ),
    [ITEM_TYPES.LAST_PAGE_LINK]: ({ isActive, onClick }) => (
      <BootstrapPagination.Last disabled={isActive} onClick={onClick}>
        <Icon name="angle-double-right" />
      </BootstrapPagination.Last>
    ),
    /* eslint-enable react/prop-types */
  },
});

const Pagination = ({
  currentPage,
  totalPages,
  boundaryPagesRange,
  siblingPagesRange,
  hideEllipsis,
  hidePreviousAndNextPageLinks,
  hideFirstAndLastPageLinks,
  disabled,
  onChange,
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

Pagination.propTypes = {
  /**
   * @required
   */
  currentPage: PropTypes.number.isRequired,
  /**
   * @required
   */
  totalPages: PropTypes.number.isRequired,
  /**
   * number of always visible pages at the beginning and end
   */
  boundaryPagesRange: PropTypes.number,
  /**
   * number of always visible pages before and after the current one
   */
  siblingPagesRange: PropTypes.number,
  /**
   * boolean flag to hide ellipsis
   */
  hideEllipsis: PropTypes.bool,
  /**
   * boolean flag to hide first and last page links
   */
  hidePreviousAndNextPageLinks: PropTypes.bool,
  /**
   * number of always visible pages at the beginning and end
   */
  hideFirstAndLastPageLinks: PropTypes.bool,
  /**
   * boolean flag to disable all buttons in pagination
   */
  disabled: PropTypes.bool,
  /**
   * callback that will be called with new page when it should be changed by user interaction (optional)
   * @returns {nextPageNumber: number}
   */
  onChange: PropTypes.func,
};

Pagination.defaultProps = {
  boundaryPagesRange: 1,
  siblingPagesRange: 1,
  hideEllipsis: false,
  hidePreviousAndNextPageLinks: false,
  hideFirstAndLastPageLinks: false,
  disabled: false,
  onChange: () => {},
};

export default Pagination;
