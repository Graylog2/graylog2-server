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
import styled, { css } from 'styled-components';

import IconButton from 'components/common/IconButton';
import Select from 'components/common/Select';

type Props = {
  currentPage: number;
  totalPages: number;
  disabled?: boolean;
  onChange?: (nextPage: number) => void;
};

const Container = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    justify-content: center;
    gap: ${theme.spacings.xs};
    margin-top: 10px;
    overflow: hidden;
    padding: 2px;
  `,
);

const PageSelectWrapper = styled.div`
  min-width: 80px;
`;

const StyledIconButton = styled(IconButton)`
  border: 1px solid ${({ theme }) => theme.colors.input.border};
  height: 29px;
  width: 29px;
`;

/**
 * Space-saving alternative to `Pagination`, meant for narrow views.
 * Renders a page select with previous/next buttons instead of a list of page buttons.
 */
const CompactPagination = ({ currentPage, totalPages, disabled = false, onChange = () => {} }: Props) => {
  const pageOptions = useMemo(
    () => Array.from({ length: totalPages }, (_, index) => ({ label: String(index + 1), value: index + 1 })),
    [totalPages],
  );

  if (totalPages <= 1 || currentPage > totalPages) {
    return null;
  }

  return (
    <Container data-testid="graylog-compact-pagination">
      <StyledIconButton
        name="chevron_left"
        title="Previous page"
        disabled={disabled || currentPage <= 1}
        onClick={() => onChange(currentPage - 1)}
      />
      <PageSelectWrapper>
        <Select
          aria-label="Select page"
          options={pageOptions}
          value={currentPage}
          onChange={(page: number) => onChange(Number(page))}
          clearable={false}
          disabled={disabled}
          size="small"
        />
      </PageSelectWrapper>
      of {totalPages}
      <StyledIconButton
        name="chevron_right"
        onClick={() => onChange(currentPage + 1)}
        title="Next page"
        disabled={disabled || currentPage >= totalPages}
      />
    </Container>
  );
};

export default CompactPagination;
