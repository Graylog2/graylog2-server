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
import { useState, useRef } from 'react';
import * as React from 'react';
import styled, { keyframes, css } from 'styled-components';

import Popover from 'components/common/Popover';

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
  top: ${$positionY};
  left: calc(${$positionX} - ${HOTSPOT_HEIGHT / 2}px);
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
  const showPopover = () => setShow(true);
  const hidePopover = () => setShow(false);

  return (
    <HotspotContainer $positionX={positionX} $positionY={positionY} ref={container}>

      <Popover opened={show} position="bottom" id="session-badge-details" width={275}>
        <Popover.Target>
          <HotspotTrigger onMouseOver={showPopover}
                          onMouseOut={hidePopover}
                          onFocus={showPopover}
                          onBlur={hidePopover}
                          ref={target}>
            {index + 1}
          </HotspotTrigger>
        </Popover.Target>
        <Popover.Dropdown>
          {children}
        </Popover.Dropdown>
      </Popover>
    </HotspotContainer>
  );
};

export default Hotspot;
