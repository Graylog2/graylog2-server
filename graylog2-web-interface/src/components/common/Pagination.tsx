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
import { Pagination as MantinePagination } from '@mantine/core';
import styled from 'styled-components';

import Icon from './Icon';

type Props = {
  currentPage: number;
  totalPages: number;
  boundaryPagesRange?: number;
  siblingPagesRange?: number;
  hidePreviousAndNextPageLinks?: boolean;
  hideFirstAndLastPageLinks?: boolean;
  disabled?: boolean;
  onChange?: (nextPage: number) => void;
  warnIfPageOutOfBounds?: boolean;
};

const controlAriaLabels = {
  first: 'Open first page',
  previous: 'Open previous page',
  next: 'Open next page',
  last: 'Open last page',
};

const PrevIcon = () => <Icon name="chevron_left" />;
const NextIcon = () => <Icon name="chevron_right" />;
const FirstIcon = () => <Icon name="keyboard_double_arrow_left" />;
const LastIcon = () => <Icon name="keyboard_double_arrow_right" />;

const StyledPagination = styled(MantinePagination)`
  margin-top: 10px;
  overflow: hidden;
  padding-bottom: 2px;

  .mantine-Group-root {
    gap: 0;
    justify-content: center;
  }

  .mantine-Pagination-control {
    border-color: ${({ theme }) => theme.colors.input.border};

    &:not([data-active]) {
      background-color: transparent;
    }

    & + .mantine-Pagination-control {
      border-left: none;
    }
  }
`;

const Pagination = ({
  currentPage,
  totalPages,
  boundaryPagesRange = 1,
  siblingPagesRange = 1,
  hidePreviousAndNextPageLinks = false,
  hideFirstAndLastPageLinks = false,
  disabled = false,
  onChange = () => {},
  warnIfPageOutOfBounds = true,
}: Props) => {
  if (totalPages <= 1) {
    return null;
  }

  if (currentPage > totalPages) {
    if (warnIfPageOutOfBounds) {
      // eslint-disable-next-line no-console
      console.warn('Pagination: `currentPage` prop should not be larger than `totalPages` prop.');
    }

    return null;
  }

  return (
    <StyledPagination
      value={currentPage}
      total={totalPages}
      boundaries={boundaryPagesRange}
      siblings={siblingPagesRange}
      withControls={!hidePreviousAndNextPageLinks}
      withEdges={!hideFirstAndLastPageLinks}
      disabled={disabled}
      onChange={onChange}
      previousIcon={PrevIcon}
      nextIcon={NextIcon}
      firstIcon={FirstIcon}
      lastIcon={LastIcon}
      getControlProps={(control) => ({ 'aria-label': controlAriaLabels[control] })}
      getItemProps={(page) => ({
        'aria-label': `Open page ${page}`,
        ...(page === currentPage ? { title: 'Active page' } : {}),
      })}
      data-testid="graylog-pagination"
    />
  );
};

export default Pagination;
