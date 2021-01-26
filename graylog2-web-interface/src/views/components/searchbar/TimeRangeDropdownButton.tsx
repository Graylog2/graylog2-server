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
import { useRef } from 'react';
import { Overlay } from 'react-overlays';

import { Button } from 'components/graylog';
import { Icon } from 'components/common';

type Props = {
  children: React.ReactNode,
  disabled?: boolean,
  hasErrorOnMount?: boolean,
  show?: boolean,
  toggleShow: () => void,
};

const TimeRangeDropdownButton = ({ children, disabled, hasErrorOnMount, show, toggleShow }: Props) => {
  const containerRef = useRef();

  return (
    <div ref={containerRef}>
      <Button bsStyle={hasErrorOnMount ? 'danger' : 'info'}
              disabled={disabled}
              onClick={toggleShow}>
        <Icon name={hasErrorOnMount ? 'exclamation-triangle' : 'clock'} />
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
  hasErrorOnMount: false,
  disabled: false,
  show: false,
};

export default TimeRangeDropdownButton;
