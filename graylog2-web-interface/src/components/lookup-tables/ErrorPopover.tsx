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

import { OverlayTrigger, Icon } from 'components/common';
import { Popover } from 'components/bootstrap';

import Styles from './ErrorPopover.css';

type Props = {
  errorText: string,
  title: string,
  placement: 'bottom' | 'top' | 'right' | 'left',
};

const ErrorPopover = ({ errorText, title = 'Error', placement = 'bottom' }: Props) => {
  const overlay = (
    <Popover id="error-popover" title={title} className={Styles.overlay}>
      {errorText}
    </Popover>
  );

  return (
    <OverlayTrigger trigger={['hover', 'focus']} placement={placement} overlay={overlay}>
      <span className={Styles.trigger}>
        <Icon name="exclamation-triangle" className="text-danger" />
      </span>
    </OverlayTrigger>
  );
};

export default ErrorPopover;
