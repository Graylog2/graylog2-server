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
import { useRef, useState } from 'react';
import styled from 'styled-components';
import { Overlay, Transition } from 'react-overlays';
import { useDisclosure } from '@mantine/hooks';

import useClickOutside from 'hooks/useClickOutside';

type Triggers = 'click' | 'focus' | 'hover';

type Props = {
  testId?: string,
  children: React.ReactElement,
  overlay: React.ReactElement,
  placement: 'top' | 'right' | 'bottom' | 'left',
  trigger?: Triggers | Array<Triggers>,
  className?: string,
  container?: React.ReactElement,
  rootClose?: boolean,
}

const TriggerWrap = styled.span`
  display: inline-block;
`;

const Container = styled.div`
  display: inline-block;
`;

const OverlayTrigger = ({ children, container, placement, overlay, rootClose, trigger, testId, className, ...overlayProps }: Props) => {
  const [opened, { close, open, toggle }] = useDisclosure(false);
  const containerRef = useRef();

  const [dropdown, setDropdown] = useState<HTMLDivElement | null>(null);
  const [control, setControl] = useState<HTMLSpanElement | null>(null);
  useClickOutside(rootClose ? close : () => {}, null, [control, dropdown]);

  const hover = trigger === 'hover' || trigger?.includes?.('hover');
  const focus = trigger === 'focus' || trigger?.includes?.('focus');
  const click = trigger === 'click' || trigger?.includes?.('click');

  return (
    <Container ref={containerRef} data-testid={testId} className={className}>
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

      {opened && (
      <Overlay show={opened}
               container={container ?? containerRef.current}
               containerPadding={10}
               placement={placement}
               shouldUpdatePosition
               target={control}
               transition={Transition}
               {...overlayProps}>
        <div ref={setDropdown}>
          {overlay}
        </div>
      </Overlay>
      )}
    </Container>
  );
};

OverlayTrigger.defaultProps = {
  trigger: 'click',
  rootClose: false,
  container: null,
  testId: undefined,
  className: undefined,
};

export default OverlayTrigger;
