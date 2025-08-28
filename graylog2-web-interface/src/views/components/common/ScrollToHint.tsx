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
import { useEffect, useMemo, useRef, useState } from 'react';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

import Icon from 'components/common/Icon';

const HINT_VISIBILITY_DURATION_MS = 2000;
const HINT_WIDTH_PX = 200;

const ScrollHint = styled.button(
  ({ theme }) => css`
    position: fixed;
    left: calc(50% - ${HINT_WIDTH_PX / 2}px);
    top: 50px;
    color: ${theme.utils.readableColor(chroma(theme.colors.brand.tertiary).alpha(0.8).css())};
    font-size: ${theme.fonts.size.huge};
    padding: 20px;
    z-index: 2000;
    width: ${HINT_WIDTH_PX}px;
    border-radius: 10px;
    border: 0;
    background: ${chroma(theme.colors.brand.tertiary).alpha(0.8).css()};
  `,
);

const isElementVisibleInContainer = (target: HTMLElement, scrollContainer: HTMLElement) => {
  const containerRect = scrollContainer.getBoundingClientRect();
  const targetRect = target.getBoundingClientRect();

  return targetRect.top >= containerRect.top && targetRect.bottom <= containerRect.bottom;
};

type Props = {
  scrollContainer: React.RefObject<HTMLElement>;
  title: string;
  // When the dependency changes, the hint will be displayed if this component is not visible.
  ifValueChanges?: unknown;
  ifTrue?: boolean;
};

const ScrollToHint = ({ ifValueChanges = undefined, scrollContainer, title, ifTrue = true }: Props) => {
  const scrollTargetRef = useRef<HTMLDivElement | null>(null);
  const [showHint, setShowHint] = useState(false);
  const [isHovered, setIsHovered] = useState(false);
  const timeoutRef = useRef(null);

  // show the scroll hint if necessary
  useEffect(() => {
    if (
      ifTrue &&
      scrollTargetRef.current &&
      scrollContainer.current &&
      !isElementVisibleInContainer(scrollTargetRef.current, scrollContainer.current)
    ) {
      setShowHint(true);
    }
  }, [ifTrue, ifValueChanges, setShowHint, scrollContainer]);

  // hide the hint automatically
  useEffect(() => {
    if (showHint && !isHovered) {
      timeoutRef.current = setTimeout(() => setShowHint(false), HINT_VISIBILITY_DURATION_MS);
    }

    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, [showHint, isHovered]);

  const iconName = useMemo(() => {
    const currentScroll = scrollContainer.current?.scrollTop;
    const elementTop = scrollTargetRef?.current?.getBoundingClientRect().top;

    return elementTop !== undefined && currentScroll !== undefined && elementTop > currentScroll
      ? 'arrow_downward'
      : 'arrow_upward';
  }, [scrollContainer]);

  const scrollToTarget = () => {
    setShowHint(false);
    setIsHovered(false);
    if (scrollTargetRef.current) {
      scrollTargetRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <>
      <div ref={scrollTargetRef} />
      {showHint && (
        <ScrollHint
          onClick={scrollToTarget}
          aria-label={title}
          onMouseEnter={() => setIsHovered(true)}
          onMouseLeave={() => setIsHovered(false)}>
          <Icon name={iconName} />
        </ScrollHint>
      )}
    </>
  );
};

export default ScrollToHint;
