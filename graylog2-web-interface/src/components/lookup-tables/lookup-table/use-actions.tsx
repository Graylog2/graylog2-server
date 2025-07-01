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

import { MenuItem, DeleteMenuItem, DropdownButton } from 'components/bootstrap';
import { Icon } from 'components/common';

import type { LookupTableEntity } from './types';

type ActionsProps = {
  lut: LookupTableEntity;
};

function Actions({ lut }: ActionsProps) {
  return (
    <DropdownButton bsStyle="transparent" title={<Icon name="more_horiz" size="lg" />} id={lut.id} noCaret pullRight>
      <MenuItem>Edit</MenuItem>
      <MenuItem divider />
      <DeleteMenuItem>Delete</DeleteMenuItem>
    </DropdownButton>
  );
}

function useActions() {
  return {
    renderActions: (lut: LookupTableEntity) => <Actions lut={lut} />,
  };
}

export default useActions;
