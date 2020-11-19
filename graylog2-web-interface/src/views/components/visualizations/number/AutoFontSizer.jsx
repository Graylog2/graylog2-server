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
// @flow strict
import React, { type Element, type ElementRef, useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import type { StyledComponent } from 'styled-components';

/**
 * This component will calculate the largest possible font size for the provided child.
 * The calculation is based on the ratio of the current dimensions of the child and the dimensions of its container.
 * The font size is being multiplied by this ratio, unless it has a difference to 1 that is smaller than the defined tolerance.
 */

const TOLERANCE = 0.05;
const CHILD_SIZE_RATIO = 0.8; // Proportion of the child size in relation to the container

const FontSize: StyledComponent<{ fontSize: number }, {}, HTMLDivElement> = styled.div`
  height: 100%;
  width: 100%;
  font-size: ${(props) => `${props.fontSize}px`};
`;

type Props = {
  children: Element<any>,
  target?: ElementRef<any>,
  height: number,
  width: number,
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

const AutoFontSizer = ({ children, target, height, width }: Props) => {
  const _container = useRef<?HTMLElement>();
  const fontSize = useAutoFontSize(target, _container, height, width);
  // $FlowFixMe: non-ideal react type declaration requires forced casting
  const _mixedContainer: { current: mixed } = _container;

  return (
    <FontSize fontSize={fontSize} ref={_mixedContainer}>
      {children}
    </FontSize>
  );
};

AutoFontSizer.defaultProps = {
  target: null,
};

export default AutoFontSizer;
