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

import Button from 'components/bootstrap/Button';
import { Icon } from 'components/common';

type Props = {
  disabled?: boolean,
  onClick: () => void,
}

const SaveAsDashboardButton = ({ disabled, onClick }: Props) => (
  <Button onClick={onClick}
          disabled={disabled}
          title="Save as new dashboard">
    <Icon name="copy" /> Save as
  </Button>
);

SaveAsDashboardButton.defaultProps = {
  disabled: false,
};

export default SaveAsDashboardButton;
