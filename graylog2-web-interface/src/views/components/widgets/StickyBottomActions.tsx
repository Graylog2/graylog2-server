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
import { useRef } from 'react';
import styled, { css } from 'styled-components';

import useIntersectionObserver from 'hooks/useIntersectionObserver';
import ScrollShadow from 'theme/box-shadows/ScrollShadow';

const Container = styled.div`
  height: 100%;
  display: flex;
  flex-flow: column nowrap;
`;

const ScrollContainer = styled.div`
  overflow-y: auto;
`;

const Content = styled.div`
  position: relative;
`;

const Actions = styled.div<{ $scrolledToBottom: boolean; $alignAtBottom: boolean }>(
  ({ theme, $scrolledToBottom, $alignAtBottom }) => css`
    position: sticky;
    width: 100%;
    bottom: 0;
    background: ${theme.colors.global.contentBackground};
    z-index: 1;
    display: flex;
    flex-direction: column;
    flex: 1;
    justify-content: ${$alignAtBottom ? 'flex-end' : 'space-between'};
    padding-top: 5px;

    ${ScrollShadow('top')}
    &::before {
      display: ${$scrolledToBottom ? 'block' : 'none'};
    }
  `,
);

const ScrolledToBottomIndicator = styled.div`
  width: 1px;
  position: absolute;
  bottom: 0;
  height: 5px;
  z-index: 0;
`;

type Props = {
  actions: React.ReactNode;
  children: React.ReactNode;
  className?: string;
  // Set this to align the actions at the bottom of the free space
  alignActionsAtBottom?: boolean;
};

const StickyBottomActions = ({ actions, children, className = undefined, alignActionsAtBottom = false }: Props) => {
  const scrollContainerRef = useRef<HTMLDivElement>();
  const scrolledToBottomIndicatorRef = useRef<HTMLDivElement>();
  const scrolledToBottom = useIntersectionObserver(scrollContainerRef, scrolledToBottomIndicatorRef);

  return (
    <Container className={className}>
      <ScrollContainer ref={scrollContainerRef}>
        <Content>
          {children}
          <ScrolledToBottomIndicator ref={scrolledToBottomIndicatorRef} />
        </Content>
      </ScrollContainer>
      <Actions $scrolledToBottom={scrolledToBottom} $alignAtBottom={alignActionsAtBottom}>
        {actions}
      </Actions>
    </Container>
  );
};

export default StickyBottomActions;
