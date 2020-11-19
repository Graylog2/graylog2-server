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
import styled, { css } from 'styled-components';
import classNames from 'classnames';

import { Row } from 'components/graylog';

const StyledRow = styled(Row)(({ theme }) => css`
  padding-bottom: 0;

  p {
    margin-top: 15px;
  }

  .actions-lg {
    float: right;
  }

  .actions-sm {
    padding-bottom: 15px;
  }

  .description-tooltips .fa-stack {
    margin-right: 3px;
  }

  .btn-lg {
    font-size: ${theme.fonts.size.large};
  }
`);

const ContentHeadRow = ({ children, className, ...props }) => {
  return (
    <StyledRow className={classNames('content-head', className)} {...props}>
      {children}
    </StyledRow>
  );
};

ContentHeadRow.propTypes = {
  children: PropTypes.node.isRequired,
  className: PropTypes.string,
};

ContentHeadRow.defaultProps = {
  className: undefined,
};

export default ContentHeadRow;
