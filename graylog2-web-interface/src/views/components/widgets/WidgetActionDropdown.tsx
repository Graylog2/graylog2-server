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
import styled, { css } from 'styled-components';

import { Button } from 'components/bootstrap';
import ActionDropdown from 'views/components/common/ActionDropdown';
import { Icon } from 'components/common';

const StyledButton = styled(Button)`
  height: 25px;
  width: 25px;
`;

const WidgetActionDropdown = ({
  children,
  onChange,
}: React.PropsWithChildren<{ onChange?: (isOpen: boolean) => void }>) => {
  const widgetActionDropdownCaret = (
    <StyledButton title="Open actions dropdown" bsSize="xs">
      <Icon name="keyboard_arrow_down" />
    </StyledButton>
  );

  return (
    <ActionDropdown element={widgetActionDropdownCaret} onChange={onChange}>
      {children}
    </ActionDropdown>
  );
};

export default WidgetActionDropdown;
