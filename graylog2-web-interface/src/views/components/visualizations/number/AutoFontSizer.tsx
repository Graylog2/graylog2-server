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
import React, { useEffect, useRef, useState } from 'react';
import styled, { css } from 'styled-components';

/**
 * This component will calculate the largest possible font size for the provided child.
 * The calculation is based on the ratio of the current dimensions of the child and the dimensions of its container.
 * The font size is being multiplied by this ratio, unless it has a difference to 1 that is smaller than the defined tolerance.
 */

const TOLERANCE = 0.05;
const CHILD_SIZE_RATIO = 0.8; // Proportion of the child size in relation to the container

const FontSize = styled.div<{ fontSize: number, $center: boolean }>`
  height: 100%;
  width: 100%;
  font-size: ${(props) => css`${props.fontSize}px`};
  ${(props) => (props.$center ? css`
    display: flex;
    justify-content: center;
    align-items: center;
  ` : '')}
`;

type ElementWithDimensions = {
  offsetHeight: number;
  offsetWidth: number;
};

type Props = {
  children: React.ReactElement,
  target?: React.Ref<any> | ElementWithDimensions,
  height: number,
  width: number,
  center?: boolean,
};

const _multiplierForElement = (element, targetWidth, targetHeight) => {
  const contentWidth = element.offsetWidth;
  const contentHeight = element.offsetHeight;

  const widthMultiplier = (targetWidth * CHILD_SIZE_RATIO) / contentWidth;
  const heightMultiplier = (targetHeight * CHILD_SIZE_RATIO) / contentHeight;

  return Math.min(widthMultiplier, heightMultiplier);
};

const isValidFontSize = (fontSize) => fontSize !== 0 && Number.isFinite(fontSize);

const useAutoFontSize = (target, _container, height, width) => {
  const [fontSize, setFontSize] = useState(20);

  useEffect(() => {
    const container = target ? { current: { children: [target] } } : _container;
    const containerChildren = container?.current?.children;

    if (!containerChildren || containerChildren.length <= 0) {
      return;
    }

    const contentElement = containerChildren[0];
    const multiplier = _multiplierForElement(contentElement, width, height);

    if (Math.abs(1 - multiplier) <= TOLERANCE) {
      return;
    }

    const newFontSize = Math.floor(fontSize * multiplier);

    if (newFontSize !== fontSize && isValidFontSize(newFontSize)) {
      setFontSize(newFontSize);
    }
  }, [target, _container, fontSize, height, width]);

  return fontSize;
};

const AutoFontSizer = ({ children, target, height, width, center }: Props) => {
  const _container = useRef<HTMLElement | undefined>();
  const fontSize = useAutoFontSize(target, _container, height, width);
  const _mixedContainer: { current } = _container;

  return (
    <FontSize $center={center} fontSize={fontSize} ref={_mixedContainer}>
      {children}
    </FontSize>
  );
};

AutoFontSizer.defaultProps = {
  target: null,
  center: false,
};

export default AutoFontSizer;
