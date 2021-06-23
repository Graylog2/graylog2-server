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
import { createRef } from 'react';
import { Overlay, Transition } from 'react-overlays';
import styled from 'styled-components';

type Triggers = 'click' | 'focus' | 'hover';

type Props = {
  children: React.ReactElement,
  overlay: React.ReactElement,
  placement: 'top' | 'right' | 'bottom' | 'left',
  trigger?: Triggers | Array<Triggers>,
  container?: React.ReactElement,
  rootClose?: boolean,
}

type State = {
  show: boolean,
}

const TriggerWrap = styled.span`
  display: inline-block;
`;

const Container = styled.div`
  display: inline-block;
`;

class OverlayTrigger extends React.Component<Props, State> {
  targetRef = createRef<HTMLButtonElement>();

  containerRef = createRef<HTMLElement>();

  static defaultProps = {
    trigger: 'click',
    rootClose: false,
    container: null,
  }

  constructor(props) {
    super(props);

    this.state = {
      show: false,
    };
  }

  hide() {
    this.setState({ show: false });
  }

  render() {
    let triggerables;
    const { children, container, placement, overlay, rootClose, trigger, ...overlayProps } = this.props;
    const { show } = this.state;

    const setShow = (isShown: boolean) => this.setState({ show: isShown });
    const toggleShow = () => setShow(!show);

    const _onTrigger = {
      click: {
        onClick: toggleShow,
      },
      hover: {
        onMouseEnter: () => setShow(true),
        onMouseLeave: () => setShow(false),
      },
      focus: {
        onFocus: () => setShow(true),
        onBlur: () => setShow(false),
      },
    };

    if (Array.isArray(trigger)) {
      const multipleTriggers = {};

      trigger.forEach((triggerType) => {
        triggerables = Object.assign(multipleTriggers, _onTrigger[triggerType]);
      });
    } else {
      triggerables = _onTrigger[trigger];
    }

    return (
      <Container ref={() => this.containerRef}>
        <TriggerWrap ref={this.targetRef} className={children.props.className} role="button">
          {React.cloneElement(children, { ...triggerables, className: '' })}
        </TriggerWrap>

        {show && (
          <Overlay show={show}
                   container={container ?? this.containerRef.current}
                   containerPadding={10}
                   placement={placement}
                   shouldUpdatePosition
                   rootClose={rootClose}
                   target={this.targetRef.current}
                   transition={Transition}
                   onHide={toggleShow}
                   {...overlayProps}>
            {overlay}
          </Overlay>
        )}
      </Container>
    );
  }
}

export default OverlayTrigger;
