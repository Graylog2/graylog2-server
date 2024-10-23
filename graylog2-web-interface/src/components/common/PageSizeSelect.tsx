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
import styled from 'styled-components';

import { DropdownButton, MenuItem } from 'components/bootstrap';
import { DEFAULT_PAGE_SIZES } from 'hooks/usePaginationQueryParameter';

const StyledDropdownButton = styled(DropdownButton)`
  ~ .dropdown-menu {
    min-width: auto;
  }
`;

const Container = styled.div`
  display: flex;
  align-items: center;
  gap: 3px;
`;

type Props = {
  className?: string,
  pageSize: number,
  pageSizes?: Array<number>,
  onChange: (newPageSize: number) => void,
  showLabel?: boolean
};

const PageSizeSelect = ({ pageSizes = DEFAULT_PAGE_SIZES, pageSize, onChange, className = '', showLabel = true }: Props) => {
  const select = (
    <StyledDropdownButton className={className}
                          id="page-size-select"
                          title={`${pageSize} Rows`}
                          aria-label="Configure page size"
                          pullRight
                          bsSize="small"
                          bsStyle="default">
      {pageSizes.map((size) => <MenuItem key={`option-${size}`} onSelect={() => onChange(size)}>{size}</MenuItem>)}
    </StyledDropdownButton>
  );

  if (showLabel) {
    return (
      <Container className={className}>
        Show
        {select}
      </Container>
    );
  }

  return select;
};

export default PageSizeSelect;
