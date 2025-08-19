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
import React from 'react';

import Popover from 'components/common/Popover';
import type { Pos } from 'views/components/visualizations/hooks/usePlotOnClickPopover';

type Props = React.PropsWithChildren<{
  isPopoverOpen: boolean;
  onPopoverChange: (isOpen: boolean) => void;
  pos: Pos;
}>;

const OnClickPopoverWrapper = ({ children = null, isPopoverOpen, onPopoverChange, pos }: Props) => (
  <Popover opened={isPopoverOpen} onChange={onPopoverChange} withArrow withinPortal position="bottom" offset={0}>
    <Popover.Target>
      <div
        style={{
          position: 'fixed',
          left: pos?.left,
          top: pos?.top,
          width: 1,
          height: 1,
        }}
      />
    </Popover.Target>
    {children}
  </Popover>
);
export default OnClickPopoverWrapper;
