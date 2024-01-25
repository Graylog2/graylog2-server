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

const StyledDescriptionBox = styled.div(({ theme }) => css`
  background-color: ${theme.colors.variant.lightest.default};
  border: 1px solid ${theme.colors.variant.lighter.default};
  padding: 10px;
  margin: 5px;
  border-radius: 6px;

  .description {
    padding-bottom: 5px;
    text-transform: uppercase;
  }
`);

type Props = React.PropsWithChildren<{
  description: React.ReactNode,
}>;

const DescriptionBox = ({ description, children }: Props) => (
  <StyledDescriptionBox>
    <div className="description">
      {description}
    </div>
    {children}
  </StyledDescriptionBox>
);

DescriptionBox.propTypes = {
  children: PropTypes.node.isRequired,
  description: PropTypes.string.isRequired,
};

export default DescriptionBox;
