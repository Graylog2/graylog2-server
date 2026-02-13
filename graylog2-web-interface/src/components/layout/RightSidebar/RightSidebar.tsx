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
import Icon from 'components/common/Icon';
import IconButton from 'components/common/IconButton';
import Row from 'components/bootstrap/Row';

const COLLAPSED_WIDTH = 36;

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
    padding: 10px;
  `,
);

const CollapsedContainer = styled.div(
  ({ theme }) => css`
    width: ${COLLAPSED_WIDTH}px;
    min-width: ${COLLAPSED_WIDTH}px;
    flex-shrink: 0;
    align-self: stretch;
    position: sticky;
    top: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    background-color: ${theme.colors.global.contentBackground};
  `,
);

const CollapsedTitleArea = styled.div(
  ({ theme }) => css`
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    cursor: pointer;
    overflow: hidden;

    :hover {
      background-color: ${theme.colors.variant.lightest.default};
    }
  `,
);

const CollapsedTitle = styled.span(
  ({ theme }) => css`
    writing-mode: vertical-rl;
    text-orientation: mixed;
    transform: rotate(180deg);
    font-size: ${theme.fonts.size.small};
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-height: 100%;
    color: ${theme.colors.text.primary};
    user-select: none;
  `,
);

const Header = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-left: 15px;
  padding-right: 15px;
`;

const NavigationButtons = styled.div`
  display: flex;
  gap: 4px;
  margin-right: 8px;
`;

const HeaderActions = styled.div`
  display: flex;
  align-items: center;
  gap: 4px;
`;

const CollapseButton = styled.button(
  ({ theme }) => css`
    background: none;
    border: none;
    padding: 4px;
    cursor: pointer;
    color: ${theme.colors.text.primary};
    display: flex;
    align-items: center;

    :hover {
      color: ${theme.colors.variant.dark.default};
    }
  `,
);

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
  overflow: hidden auto;
  padding: 15px;
  min-height: 0;
`;

const StyledRow = styled(Row)`
  margin-left: 0;
  margin-right: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
`;

const RightSidebar = () => {
  const { content, width, isCollapsed, closeSidebar, collapseSidebar, expandSidebar, goBack, goForward, canGoBack, canGoForward } = useRightSidebar();

  if (!content) {
    return null;
  }

  if (isCollapsed) {
    return (
      <CollapsedContainer
        role="complementary"
        aria-label={`${content.title} sidebar (collapsed)`}>
        <IconButton name="close" title="Close sidebar" onClick={closeSidebar} aria-label="Close sidebar" />
        <CollapsedTitleArea onClick={expandSidebar} title="Expand sidebar">
          <CollapsedTitle>{content.title}</CollapsedTitle>
        </CollapsedTitleArea>
      </CollapsedContainer>
    );
  }

  const ContentComponent = content.component;

  return (
    <Container $width={width} role="complementary" aria-label={`${content.title} sidebar`}>
      <StyledRow className="content">
        <Header>
          <NavigationButtons>
            <IconButton
              name="arrow_back"
              title="Go back"
              onClick={goBack}
              disabled={!canGoBack}
              aria-label="Go back to previous content"
            />
            <IconButton
              name="arrow_forward"
              title="Go forward"
              onClick={goForward}
              disabled={!canGoForward}
              aria-label="Go forward to next content"
            />
          </NavigationButtons>
          <Title id="sidebar-title">{content.title}</Title>
          <HeaderActions>
            <CollapseButton type="button" title="Collapse sidebar" onClick={collapseSidebar} aria-label="Collapse sidebar">
              <Icon name="collapse_content" />
            </CollapseButton>
            <IconButton name="close" title="Close sidebar" onClick={closeSidebar} aria-label="Close sidebar" />
          </HeaderActions>
        </Header>
        <ContentArea aria-labelledby="sidebar-title">
          <ContentComponent {...(content.props || {})} />
        </ContentArea>
      </StyledRow>
    </Container>
  );
};

export default RightSidebar;
