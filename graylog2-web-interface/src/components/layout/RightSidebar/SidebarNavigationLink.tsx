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

import useRightSidebar from 'hooks/useRightSidebar';
import type { RightSidebarContent } from 'contexts/RightSidebarContext';

type Props = {
  content: RightSidebarContent;
  children: React.ReactNode;
};

const StyledButton = styled.button(
  ({ theme }) => css`
    background: none;
    border: none;
    padding: 0;
    color: ${theme.colors.variant.primary};
    text-decoration: underline;
    cursor: pointer;
    font-size: inherit;
    font-family: inherit;

    :hover {
      color: ${theme.colors.variant.dark.primary};
    }

    :focus-visible {
      outline: 2px solid ${theme.colors.variant.primary};
      outline-offset: 2px;
      border-radius: 2px;
    }

    :active {
      color: ${theme.colors.variant.darker.primary};
    }
  `,
);

const SidebarNavigationLink = ({ content, children }: Props) => {
  const { openSidebar } = useRightSidebar();

  const handleClick = () => {
    openSidebar(content);
  };

  return (
    <StyledButton type="button" onClick={handleClick}>
      {children}
    </StyledButton>
  );
};

export default SidebarNavigationLink;
