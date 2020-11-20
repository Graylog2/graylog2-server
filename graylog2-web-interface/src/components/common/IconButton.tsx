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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import Icon from 'components/common/Icon';

const Wrapper: StyledComponent<{}, ThemeInterface, HTMLButtonElement> = styled.button(({ theme }) => css`
  display: inline-flex;
  justify-content: center;
  align-items: center;
  height: 25px;
  width: 25px;
  border: 0;
  background-color: transparent;
  cursor: pointer;
  color: ${theme.colors.gray[70]};
  font-size: ${theme.fonts.size.large};

  :hover {
    background-color: ${theme.colors.gray[90]};
  }

  :active {
    background-color: ${theme.colors.gray[80]};
  }
`);

type Props = {
  focusable?: boolean,
  title: string,
  onClick?: () => void,
  className?: string,
  name: string,
};

const handleClick = (onClick) => {
  if (typeof onClick === 'function') {
    onClick();
  }
};

const IconButton = ({ title, onClick, focusable, className, ...rest }: Props) => (
  <Wrapper tabIndex={focusable ? 0 : -1} title={title} onClick={() => handleClick(onClick)} className={className} type="button">
    <Icon {...rest} />
  </Wrapper>
);

IconButton.propTypes = {
  className: PropTypes.string,
  title: PropTypes.string,
  onClick: PropTypes.func,
  name: PropTypes.string.isRequired,
};

IconButton.defaultProps = {
  className: undefined,
  focusable: true,
  onClick: undefined,
  title: undefined,
};

export default IconButton;
