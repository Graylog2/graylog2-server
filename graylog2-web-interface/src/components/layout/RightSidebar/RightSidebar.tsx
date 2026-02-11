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
import ContentHeadRow from 'components/common/ContentHeadRow';

const Container = styled.div<{ $width: number }>(
  ({ $width }) => css`
    width: ${$width}px;
    min-width: ${$width}px;
    flex-shrink: 0;
    align-self: stretch;
    position: sticky;
    top: 0;
    display: flex;
    flex-direction: column;
    z-index: 1020;
    padding: 10px;
  `,
);

const Header = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-left: 15px;
`;

const Title = styled.h4(
  ({ theme }) => css`
    margin: 0;
    font-size: ${theme.fonts.size.h1};
    font-weight: normal;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
  `,
);

const ContentArea = styled.div`
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 15px;
  min-height: 0;
`;

const RightSidebar = () => {
  const { content, width, closeSidebar } = useRightSidebar();

  if (!content) {
    return null;
  }

  const ContentComponent = content.component;

  return (
    <Container $width={width} role="complementary" aria-label={`${content.title} sidebar`}>
      <ContentHeadRow className="content">
        <Header>
          <Title id="sidebar-title">{content.title}</Title>
          <IconButton name="close" title="Close sidebar" onClick={closeSidebar} aria-label="Close sidebar" />
        </Header>
        <ContentArea aria-labelledby="sidebar-title">
          <ContentComponent {...(content.props || {})} />
        </ContentArea>
      </ContentHeadRow>
    </Container>
  );
};

export default RightSidebar;
