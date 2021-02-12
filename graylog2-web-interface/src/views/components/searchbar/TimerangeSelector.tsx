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
import styled, { css } from 'styled-components';

const TimerangeWrap = styled.div(({ theme }) => css`
  .form-group {
    margin-bottom: 0;
  }

  .relative {
    color: ${theme.colors.gray[40]};
  }

  .absolute {
    font-size: ${theme.fonts.size.body};
  }
`);

type Props = {
  className?: string,
  children: React.ReactNode
  style?: React.CSSProperties
}

const TimerangeSelector = ({ className, children, ...restProps }: Props) => {
  return (
    <TimerangeWrap className={className} {...restProps}>
      {children}
    </TimerangeWrap>
  );
};

TimerangeSelector.propTypes = {
  className: PropTypes.string,
  children: PropTypes.node.isRequired,
  style: PropTypes.object,
};

TimerangeSelector.defaultProps = {
  className: undefined,
  style: undefined,
};

export default TimerangeSelector;
