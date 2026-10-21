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
import { useMemo } from 'react';
import { Pagination as MantinePagination } from '@mantine/core';
import styled, { css, useTheme } from 'styled-components';

import Icon from 'components/common/Icon';
import Select, { CONTROL_CLASS } from 'components/common/Select';

type Props = {
  currentPage: number;
  totalPages: number;
  disabled?: boolean;
  onChange?: (nextPage: number) => void;
};

const CONTROL_HEIGHT = '26px';

const PrevIcon = () => <Icon name="chevron_left" />;
const NextIcon = () => <Icon name="chevron_right" />;

const Container = styled(MantinePagination.Root)(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: ${theme.spacings.xs};
    font-size: ${theme.fonts.size.small};

    /* PaginatedList centers its pagination, which would also center the select value and options. */
    text-align: left;

    /* Matches the line height of the Mantine pagination controls, so the text is centered like their icons. */
    line-height: 1;
    margin-top: 10px;
    overflow: hidden;
    padding: 2px;

    .mantine-Pagination-control {
      border-color: ${theme.colors.input.border};
      background-color: transparent;
    }
  `,
);

/* The small select is 29px high, this aligns it with the 26px high pagination controls. */
const PageSelectWrapper = styled.div`
  min-width: 50px;

  .${CONTROL_CLASS} {
    min-height: ${CONTROL_HEIGHT};
    height: ${CONTROL_HEIGHT};
  }
`;

/**
 * Space-saving alternative to `Pagination`, meant for narrow views.
 * Renders a page select between previous/next buttons instead of a list of page buttons.
 */
const CompactPagination = ({ currentPage, totalPages, disabled = false, onChange = () => {} }: Props) => {
  const theme = useTheme();
  const pageOptions = useMemo(
    () => Array.from({ length: totalPages }, (_, index) => ({ label: String(index + 1), value: index + 1 })),
    [totalPages],
  );

  if (totalPages <= 1 || currentPage > totalPages) {
    return null;
  }

  return (
    <Container
      total={totalPages}
      value={currentPage}
      onChange={onChange}
      disabled={disabled}
      size="sm"
      data-testid="graylog-compact-pagination">
      <MantinePagination.Previous icon={PrevIcon} aria-label="Open previous page" title="Previous page" />
      <PageSelectWrapper>
        <Select
          aria-label="Select page"
          options={pageOptions}
          value={currentPage}
          onChange={(page: number) => onChange(Number(page))}
          clearable={false}
          disabled={disabled}
          size="small"
          styles={{
            singleValue: (base) => ({ ...base, lineHeight: CONTROL_HEIGHT }),
            valueContainer: (base) => ({ ...base, padding: '0 2px' }),
            dropdownIndicator: (base, state) => ({
              ...base,
              padding: 0,
              fontSize: '150%',
              marginRight: theme.spacings.xs,
              transform: state.selectProps.menuIsOpen && 'rotate(180deg)',
            }),
          }}
        />
      </PageSelectWrapper>
      <span>of {totalPages}</span>
      <MantinePagination.Next icon={NextIcon} aria-label="Open next page" title="Next page" />
    </Container>
  );
};

export default CompactPagination;
