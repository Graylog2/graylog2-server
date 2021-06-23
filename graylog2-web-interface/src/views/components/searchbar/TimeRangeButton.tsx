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
import { SyntheticEvent } from 'react';

import { Button } from 'components/graylog';
import { Icon } from 'components/common';

type Props = {
  hasError?: boolean
  disabled?: boolean,
  onClick?: (e: SyntheticEvent) => void,
};

const TimeRangeButton = ({ hasError, disabled, onClick }: Props) => (
  <Button bsStyle={hasError ? 'danger' : 'info'}
          disabled={disabled}
          onClick={onClick}
          aria-label="Open Time Range Selector">
    <Icon name={hasError ? 'exclamation-triangle' : 'clock'} />
  </Button>
);

TimeRangeButton.defaultProps = {
  hasError: false,
  disabled: false,
  onClick: undefined,
};

export default TimeRangeButton;
