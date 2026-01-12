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

import { MenuItem } from 'components/bootstrap';
import OverlayDropdownButton from 'components/common/OverlayDropdownButton';

type Props = {
  onChangeSlicing: (event: React.MouseEvent<HTMLDivElement>) => void;
};

const HeaderActionsDropdownButton = ({ onChangeSlicing }: Props) => (
  <OverlayDropdownButton
    title="Toggle column actions"
    buttonTitle="Toggle column actions"
    bsSize="xsmall"
    triggerVariant="icon_horizontal">
    <MenuItem onClick={onChangeSlicing}>Slice by values</MenuItem>
  </OverlayDropdownButton>
);

export default HeaderActionsDropdownButton;
