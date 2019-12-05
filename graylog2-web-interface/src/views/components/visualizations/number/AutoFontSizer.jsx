// @flow strict
import React, { type Element, type ElementRef, type AbstractComponent, useEffect, useRef, useState } from 'react';
import styled from 'styled-components';

const FontSize: AbstractComponent<{ fontSize: number }> = styled.div`
  height: 100%;
  width: 100%;
  font-size: ${props => `${props.fontSize}px`};
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

  const widthMultiplier = (targetWidth * 0.8) / contentWidth;
  const heightMultiplier = (targetHeight * 0.8) / contentHeight;
  return Math.min(widthMultiplier, heightMultiplier);
};

const AutoFontSizer = ({ children, target, height, width }: Props) => {
  const [fontSize, setFontSize] = useState(20);
  const _container = useRef<?HTMLElement>();

  useEffect(() => {
    const container = target ? { current: { children: [target] } } : _container;
    if (!container.current) {
      return;
    }

    const { children: containerChildren } = container.current;

    if (containerChildren.length <= 0) {
      return;
    }

    const contentElement = containerChildren[0];
    const multiplier = _multiplierForElement(contentElement, width, height);

    if (Math.abs(1 - multiplier) <= 0.01) {
      return;
    }

    const newFontsize = Math.floor(fontSize * multiplier);

    if (fontSize !== newFontsize && newFontsize !== 0 && Number.isFinite(newFontsize)) {
      // eslint-disable-next-line react/no-did-update-set-state
      setFontSize(newFontsize);
    }
  }, [target, _container, fontSize, height, width]);

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
