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
import styled, { css } from 'styled-components';

type Props = React.PropsWithChildren<{
  onClick?: (e: React.SyntheticEvent) => void;
  isActive?: boolean;
  className?: string;
  title?: string;
}>;

const Container = styled.div<{ $isActive: boolean; $isClickable: boolean }>(
  ({ theme, $isActive, $isClickable }) => css`
    border: 1px solid ${theme.colors.cards.border};
    background-color: ${theme.colors.cards.background};
    border-radius: ${theme.spacings.xs};
    padding: ${theme.spacings.sm};

    ${$isActive &&
    css`
      outline: 1px solid ${theme.colors.input.borderFocus};
    `}

    ${$isClickable &&
    css`
      cursor: pointer;

      &:hover,
      &:focus-visible {
        outline: 1px solid ${theme.colors.input.borderFocus};
        box-shadow: ${theme.colors.input.boxShadow};
      }
    `}
  `,
);

const AccessibleCard = ({
  onClick = undefined,
  isActive = false,
  children = null,
  className = undefined,
  title = undefined,
}: Props) => {
  const isClickable = typeof onClick === 'function';
  const asProp = isClickable ? 'button' : 'div';
  const typeProp = isClickable ? 'button' : undefined;

  return (
    <Container
      title={title}
      as={asProp}
      type={typeProp}
      className={className}
      $isActive={isActive}
      onClick={onClick}
      $isClickable={typeof onClick === 'function'}>
      {children}
    </Container>
  );
};

export default AccessibleCard;
