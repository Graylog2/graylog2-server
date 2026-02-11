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
import IconButton from 'components/common/IconButton';

const Container = styled.div<{ $width: number }>(
  ({ theme, $width }) => css`
    width: ${$width}px;
    min-width: ${$width}px;
    flex-shrink: 0;
    height: 100%;
    background-color: ${theme.colors.global.contentBackground};
    border-left: 1px solid ${theme.colors.variant.light.default};
    display: flex;
    flex-direction: column;
    z-index: 1020;
  `,
);

const Header = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: ${theme.spacings.sm};
    border-bottom: 1px solid ${theme.colors.variant.light.default};
    min-height: 50px;
  `,
);

const Title = styled.h3(
  ({ theme }) => css`
    margin: 0;
    font-size: ${theme.fonts.size.large};
    font-weight: normal;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  `,
);

const ContentArea = styled.div(
  ({ theme }) => css`
    flex: 1;
    overflow: auto;
    padding: ${theme.spacings.sm};
  `,
);

const RightSidebar = () => {
  const { content, width, closeSidebar } = useRightSidebar();

  if (!content) {
    return null;
  }

  const ContentComponent = content.component;

  return (
    <Container $width={width}>
      <Header>
        <Title>{content.title}</Title>
        <IconButton name="close" title="Close sidebar" onClick={closeSidebar} />
      </Header>
      <ContentArea>
        <ContentComponent {...(content.props || {})} />
      </ContentArea>
    </Container>
  );
};

export default RightSidebar;
