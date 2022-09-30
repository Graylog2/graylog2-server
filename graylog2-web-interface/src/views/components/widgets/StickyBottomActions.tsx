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
import styled, { css } from 'styled-components';
import { useState, useEffect } from 'react';

const Container = styled.div`
  position: relative;
`;

const Actions = styled.div<{ $scrolledToBottom: boolean }>(({ theme, $scrolledToBottom }) => css`
  position: sticky;
  width: 100%;
  bottom: 0;
  padding-top: 5px;
  background: ${theme.colors.global.contentBackground};
  z-index: 1;

  ::before {
    box-shadow: 1px -2px 3px rgb(0 0 0 / 25%);
    content: ' ';
    display: ${$scrolledToBottom ? 'block' : 'none'};
    height: 3px;
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
  }
`);

const ScrolledToBottomIndicator = styled.div`
  width: 1px;
  position: absolute;
  bottom: 0;
  height: 5px;
  z-index: 0;
`;

const useScrolledToBottom = (): {
  setScrolledToBottomIndicatorRef: (ref: HTMLDivElement) => void,
  scrolledToBottom: boolean
} => {
  const [scrolledToBottomIndicatorRef, setScrolledToBottomIndicatorRef] = useState(null);
  const [scrolledToBottom, setScrolledToBottom] = useState(false);

  useEffect(() => {
    const observer = new IntersectionObserver(([entry]) => {
      setScrolledToBottom(!entry.isIntersecting);
    }, { threshold: 0.9 });

    if (scrolledToBottomIndicatorRef) {
      observer.observe(scrolledToBottomIndicatorRef);
    }

    return () => {
      if (scrolledToBottomIndicatorRef) {
        observer.unobserve(scrolledToBottomIndicatorRef);
      }
    };
  }, [scrolledToBottomIndicatorRef]);

  return { setScrolledToBottomIndicatorRef, scrolledToBottom };
};

type Props = {
  actions: React.ReactNode,
  children: React.ReactNode,
}

const StickyBottomActions = ({ actions, children }: Props) => {
  const { setScrolledToBottomIndicatorRef, scrolledToBottom } = useScrolledToBottom();

  return (
    <Container>
      {children}
      <Actions $scrolledToBottom={scrolledToBottom}>
        {actions}
      </Actions>
      <ScrolledToBottomIndicator ref={setScrolledToBottomIndicatorRef} />
    </Container>
  );
};

export default StickyBottomActions;
