// @flow strict
import React, { type Element, type ElementRef, useEffect, useRef, useState } from 'react';
import styled, { type StyledComponent } from 'styled-components';

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
  const childSizeRatio = 0.8; // Propotion of the child size in relation to the container
  const contentWidth = element.offsetWidth;
  const contentHeight = element.offsetHeight;

  const widthMultiplier = (targetWidth * childSizeRatio) / contentWidth;
  const heightMultiplier = (targetHeight * childSizeRatio) / contentHeight;
  return Math.min(widthMultiplier, heightMultiplier);
};

const _updateFontSize = (setFontSize, currentSize, newSize) => {
  if (currentSize !== newSize && newSize !== 0 && Number.isFinite(newSize)) {
    setFontSize(newSize);
  }
};

const useAutoFontSize = (target, _container, height, width) => {
  // This hook will update the font size based on the difference between the dimensions of the container and its child.
  // The font size is being recalculated, until the mentioned difference is within the tolerance.
  const tolerance = 0.01;
  const [fontSize, setFontSize] = useState(20);

  useEffect(() => {
    const container = target ? { current: { children: [target] } } : _container;
    const containerChildren = container?.current?.children;

    if (!containerChildren || containerChildren.length <= 0) {
      return;
    }

    const contentElement = containerChildren[0];
    const multiplier = _multiplierForElement(contentElement, width, height);
    if (Math.abs(1 - multiplier) <= tolerance) {
      return;
    }

    const newFontSize = Math.floor(fontSize * multiplier);
    _updateFontSize(setFontSize, fontSize, newFontSize);
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
