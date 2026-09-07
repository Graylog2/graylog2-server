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
import React, { useLayoutEffect, useRef, useState } from 'react';
import styled, { css } from 'styled-components';

/**
 * This component will calculate the largest possible font size for the provided child.
 * The calculation is based on the ratio of the current dimensions of the child and the dimensions of its container.
 * The font size is being multiplied by this ratio, unless it has a difference to 1 that is smaller than the defined tolerance.
 */

const TOLERANCE = 0.05;
const CHILD_SIZE_RATIO = 1; // Proportion of the child size in relation to the container

type Alignment = 'center' | 'bottom-right' | 'bottom-left';

const FontSize = styled.div<{ fontSize: number; $alignment: Alignment | undefined }>`
  height: 100%;
  width: 100%;
  overflow: hidden;
  font-size: ${(props) => props.fontSize}px;
  line-height: 1;
  white-space: nowrap;
  ${(props) =>
    props.$alignment === 'center'
      ? css`
          display: flex;
          justify-content: center;
          align-items: center;
        `
      : ''}
  ${(props) =>
    props.$alignment === 'bottom-right'
      ? css`
          display: flex;
          justify-content: flex-end;
          align-items: flex-end;
        `
      : ''}

  ${(props) =>
    props.$alignment === 'bottom-left'
      ? css`
          display: flex;
          justify-content: flex-start;
          align-items: flex-end;
        `
      : ''}
`;

type ElementWithDimensions = {
  offsetHeight: number;
  offsetWidth: number;
};

type Props = {
  children: React.ReactElement;
  target?: React.Ref<any> | ElementWithDimensions;
  height: number;
  width: number;
  alignment?: Alignment;
};

const _multiplierForElement = (element, targetWidth, targetHeight) => {
  const contentWidth = element.offsetWidth;
  const contentHeight = element.offsetHeight;

  const widthMultiplier = (targetWidth * CHILD_SIZE_RATIO) / contentWidth;
  const heightMultiplier = (targetHeight * CHILD_SIZE_RATIO) / contentHeight;

  return Math.min(widthMultiplier, heightMultiplier);
};

const isValidFontSize = (fontSize) => fontSize !== 0 && Number.isFinite(fontSize);

const useAutoFontSize = (target, _container, height, width, children) => {
  const [fontSize, setFontSize] = useState(20);

  useLayoutEffect(() => {
    const container = target ? { current: { children: [target] } } : _container;
    const containerChildren = container?.current?.children;

    if (!containerChildren || containerChildren.length <= 0) {
      return;
    }

    const contentElement = containerChildren[0];
    const multiplier = _multiplierForElement(contentElement, width, height);
    // multiplier < 1 means the content currently overflows — tolerate being a bit smaller than the
    // container, never a bit bigger.
    const isOverflowing = multiplier < 1;

    if (!isOverflowing && multiplier - 1 <= TOLERANCE) {
      return;
    }

    let newFontSize = Math.floor(fontSize * multiplier);

    if (isOverflowing && newFontSize >= fontSize) {
      // Rounding alone didn't produce a smaller size even though we're overflowing — force it down.
      newFontSize = fontSize - 1;
    }

    if (newFontSize !== fontSize && isValidFontSize(newFontSize)) {
      // The font size is measured and iteratively adjusted after render, which requires setting state in the effect.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setFontSize(newFontSize);
    }
  }, [target, _container, fontSize, height, width, children]);

  return fontSize;
};

const AutoFontSizer = ({ children, target = null, height, width, alignment = undefined }: Props) => {
  const _container = useRef<HTMLElement | undefined>();
  const fontSize = useAutoFontSize(target, _container, height, width, children);
  const _mixedContainer: { current } = _container;

  return (
    <FontSize $alignment={alignment} fontSize={fontSize} ref={_mixedContainer}>
      {children}
    </FontSize>
  );
};

export default AutoFontSizer;
