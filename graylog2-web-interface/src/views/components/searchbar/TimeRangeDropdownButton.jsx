// @flow strict
import * as React from 'react';
import { useRef } from 'react';
import { Overlay } from 'react-overlays';

import { Button } from 'components/graylog';
import { Icon } from 'components/common';

type Props = {
  children: React.Node,
  disabled?: boolean,
  show?: boolean,
  toggleShow: (void) => boolean,
};

const TimeRangeDropdownButton = ({ children, disabled, show, toggleShow }: Props) => {
  const containerRef = useRef();

  return (
    <div ref={containerRef}>
      <Button bsStyle="info"
              disabled={disabled}
              onClick={toggleShow}>
        <Icon name="clock" />
      </Button>

      <Overlay show={show}
               trigger="click"
               placement="bottom"
               onHide={toggleShow}
               container={containerRef.current}>
        {children}
      </Overlay>
    </div>
  );
};

TimeRangeDropdownButton.defaultProps = {
  disabled: false,
  show: false,
};

export default TimeRangeDropdownButton;
