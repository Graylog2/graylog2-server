import { useState, useRef } from 'react';
import * as React from 'react';
import styled, { keyframes, css } from 'styled-components';
import { Overlay } from 'react-overlays';

import { Popover } from 'components/bootstrap';

const HOTSPOT_HEIGHT = 80;

const hotspotExpand = keyframes`
  0% {
    transform: scale(.5);
    opacity: 1;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
`;

const HotspotTrigger = styled.button(({ theme }) => css`
  && {
    border-radius: 50%;
    height: ${HOTSPOT_HEIGHT}px;
    width: ${HOTSPOT_HEIGHT}px;
    background: ${theme.colors.variant.warning};
    color: ${theme.utils.contrastingColor(theme.colors.variant.warning)};
    border: 0;
    font-size: ${theme.fonts.size.huge};
    &:hover {
      background: ${theme.colors.variant.warning};
      color: ${theme.utils.contrastingColor(theme.colors.variant.warning)};
    }
  }
  
  &::before {
    background: ${theme.colors.variant.warning};
    content: "";
    width: 100%;
    height: 100%;
    position: absolute;
    z-index: -1;
    opacity: 0;
    animation: ${hotspotExpand} 2s infinite;
    border-radius: 50%;
    left: 0;
    top: 0;
  }
`);

const HotspotContainer = styled.div<{ $positionX: string, $positionY: string }>(({ $positionX, $positionY }) => css`
  position: absolute;
  top: ${$positionX};
  left: calc(${$positionY} - ${HOTSPOT_HEIGHT / 2}px);
`);

type TooltipProps = React.PropsWithChildren<{
  positionX: string,
  positionY: string,
  index: number,
}>

const Hotspot = ({ children, positionX, positionY, index }: TooltipProps) => {
  const [show, setShow] = useState(false);
  const target = useRef();
  const container = useRef();

  return (
    <HotspotContainer $positionX={positionX} $positionY={positionY} ref={container}>
      <HotspotTrigger onMouseOver={() => setShow(true)} onMouseOut={() => setShow(false)} ref={target}>
        {index + 1}
      </HotspotTrigger>

      <Overlay show={show}
               contianer={container.current}
               target={target.current}
               shouldUpdatePosition
               placement="bottom">
        <Popover id="session-badge-details">
          {children}
        </Popover>
      </Overlay>
    </HotspotContainer>
  );
};

export default Hotspot;
