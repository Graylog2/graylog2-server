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
import type { PropsWithChildren } from 'react';
import React, { useState } from 'react';
import styled, { css } from 'styled-components';

import { Button } from 'components/bootstrap';
import { Icon } from 'components/common';
import Popover from 'components/common/Popover';

type Props = PropsWithChildren<{
  helpText: string|React.ReactNode;
}>;

const StyledButton = styled(Button)(
  ({ theme }) => css`
    padding: 1px 0;
    font-size: ${theme.fonts.size.body};
  `,
);

const StyledIcon = styled(Icon)(
  ({ theme }) => css`
    color: ${theme.colors.variant.warning};
  `,
);

const HelpPopoverButton = ({ helpText, children = null }: Props) => {
  const [showHelp, setShowHelp] = useState(false);
  const toggleHelp = () => setShowHelp((cur) => !cur);

  return (
    <Popover
      position="right"
      width={500}
      opened={showHelp}
      withArrow
      onChange={setShowHelp}
      closeOnClickOutside
      withinPortal>
      <Popover.Target>
        <StyledButton bsStyle="transparent" bsSize={children ? "xsmall" : "medium"} onClick={toggleHelp}>
          {children || <StyledIcon name="help" type="regular" />}
        </StyledButton>
      </Popover.Target>
      <Popover.Dropdown>{helpText}</Popover.Dropdown>
    </Popover>
  );
};

export default HelpPopoverButton;
