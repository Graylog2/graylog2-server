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
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Input, DropdownButton, MenuItem } from 'components/bootstrap';

const Wrapper = styled.div`
  margin-bottom: 5px;

  && .form-group {
    margin-bottom: 0;
  }

  .control-label {
    padding-top: 0;
    font-weight: normal;
  }

  .page-size-select {
    display: flex;
    align-items: baseline;
  }
`;

const PAGE_SIZES = [10, 50, 100];

type Props = {
  className?: string,
  pageSize: number,
  pageSizes: Array<number>,
  onChange: (newPageSize: number) => void,
};

const PageSizeSelect = ({ pageSizes, pageSize, onChange, className }: Props) => (
  <DropdownButton className={className}
                  title={`${pageSize} rows`}
                  bsSize="small"
                  bsStyle="default">
    {pageSizes.map((size) => <MenuItem key={`option-${size}`} onSelect={() => onChange(size)}>{size}</MenuItem>)}
  </DropdownButton>
);

PageSizeSelect.propTypes = {
  className: PropTypes.string,
  onChange: PropTypes.func.isRequired,
  pageSize: PropTypes.number.isRequired,
  pageSizes: PropTypes.arrayOf(PropTypes.number),
};

PageSizeSelect.defaultProps = {
  className: '',
  pageSizes: PAGE_SIZES,
};

PageSizeSelect.defaultPageSizes = PAGE_SIZES;

export default PageSizeSelect;
