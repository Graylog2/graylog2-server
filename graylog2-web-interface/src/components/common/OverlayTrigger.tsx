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
import { useImperativeHandle, useRef, useState } from 'react';
import styled from 'styled-components';
import { useDisclosure } from '@mantine/hooks';

import Popover from 'components/common/Popover';
import useClickOutside from 'hooks/useClickOutside';

type Triggers = 'click' | 'focus' | 'hover';

type Props = {
  disabled?: boolean,
  testId?: string,
  children: React.ReactElement,
  overlay: React.ReactNode,
  placement: 'top' | 'right' | 'bottom' | 'left',
  trigger?: Triggers | Array<Triggers>,
  className?: string,
  rootClose?: boolean,
  width?: number,
  title?: React.ReactNode,
}

const TriggerWrap = styled.span`
  display: inline-flex;
`;

const Container = styled.div`
  display: inline-flex;
`;

type OverlayType = {
  hide: () => void,
};
const OverlayTrigger = React.forwardRef<OverlayType, Props>(({ children, disabled = false, placement, overlay, rootClose = false, trigger = 'click', testId, className, title, width = 275 }, ref) => {
  const [opened, { close, open, toggle }] = useDisclosure(false);

  useImperativeHandle(ref, () => ({
    hide: close,
  }), [close]);

  // @ts-ignore
  OverlayTrigger.hide = close;

  const containerRef = useRef();

  const [dropdown, setDropdown] = useState<HTMLDivElement | null>(null);
  const [control, setControl] = useState<HTMLSpanElement | null>(null);
  useClickOutside(rootClose ? close : () => {}, null, [control, dropdown]);

  const hover = trigger === 'hover' || trigger?.includes?.('hover');
  const focus = trigger === 'focus' || trigger?.includes?.('focus');
  const click = trigger === 'click' || trigger?.includes?.('click');

  return disabled
    ? children
    : (
      <Container ref={containerRef} data-testid={testId} className={className}>
        <Popover opened={opened} withArrow width={width} position={placement} withinPortal>
          <Popover.Target>
            <TriggerWrap className={children.props.className}
                         ref={setControl}
                         role="button"
                         onClick={click ? toggle : undefined}
                         onMouseEnter={hover ? open : undefined}
                         onMouseLeave={hover ? close : undefined}
                         onFocus={focus ? open : undefined}
                         onBlur={focus ? open : undefined}>
              {children}
            </TriggerWrap>
          </Popover.Target>

          <Popover.Dropdown title={title}>
            <div ref={setDropdown}>
              {overlay}
            </div>
          </Popover.Dropdown>
        </Popover>
      </Container>
    );
});

export default OverlayTrigger;
