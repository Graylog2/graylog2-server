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
import { useEffect, useRef, useState } from 'react';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

import useScrollContainer from 'components/common/ScrollContainer/useScrollContainer';
import Icon from 'components/common/Icon';

const HINT_VISIBILITY_DURATION_MS = 2000;

const ScrollHint = styled.div(
  ({ theme }) => css`
    position: fixed;
    left: 50%;
    margin-left: -125px;
    top: 50px;
    /* stylelint-disable function-no-unknown */
    color: ${theme.utils.readableColor(chroma(theme.colors.brand.tertiary).alpha(0.8).css())};
    font-size: 80px;
    padding: 25px;
    z-index: 2000;
    width: 200px;
    text-align: center;
    cursor: pointer;
    border-radius: 10px;
    background: ${chroma(theme.colors.brand.tertiary).alpha(0.8).css()};
  `,
);

const isElementVisibleInContainer = (target: HTMLElement, container: HTMLElement) => {
  const containerRect = container.getBoundingClientRect();
  const targetRect = target.getBoundingClientRect();

  return targetRect.top >= containerRect.top && targetRect.bottom <= containerRect.bottom;
};

type Props = {
  // When the dependency changes, the hint will be displayed if this component is not visible.
  triggerDependency: unknown;
};

const ScrollToHint = ({ triggerDependency }: Props) => {
  const { container } = useScrollContainer();
  const scrollTargetRef = useRef<HTMLDivElement | null>(null);
  const [showHint, setShowHint] = useState(false);
  const [isHovered, setIsHovered] = useState(false);
  const timeoutRef = useRef(null);

  // show the scroll hint if necessary
  useEffect(() => {
    if (
      scrollTargetRef.current &&
      container.current &&
      !isElementVisibleInContainer(scrollTargetRef.current, container.current)
    ) {
      setShowHint(true);
    }
  }, [triggerDependency, setShowHint, container]);

  // hide the hint automatically
  useEffect(() => {
    if (showHint && !isHovered) {
      timeoutRef.current = setTimeout(() => setShowHint(false), HINT_VISIBILITY_DURATION_MS);
    }

    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, [showHint, isHovered]);

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
          onMouseEnter={() => setIsHovered(true)}
          onMouseLeave={() => setIsHovered(false)}>
          <Icon name="arrow_upward" />
        </ScrollHint>
      )}
    </>
  );
};

export default ScrollToHint;
