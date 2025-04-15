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
import type { ColorVariant } from '@graylog/sawmill';

import { Button } from 'components/bootstrap';
import { Icon } from 'components/common';
import Popover from 'components/common/Popover';
import type { BsSize } from 'components/bootstrap/types';

type Props = PropsWithChildren<{
  helpText: string|React.ReactNode;
  bsStyle?: ColorVariant;
  bsSize?: BsSize;
}>;

const StyledButton = styled(Button)`
  padding: 1px 0;
`;

const StyledOverlay = styled(Popover.Dropdown)`
  white-space: pre-line;
`;

const StyledIcon = styled(Icon)<{ $bsStyle: ColorVariant }>(
  ({ $bsStyle, theme }) => css`
    color: ${theme.colors.variant[$bsStyle]};
  `,
);

const HelpPopoverButton = ({ helpText, bsStyle = "info", bsSize = "medium" }: Props) => {
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
        <StyledButton bsStyle="transparent" bsSize={bsSize} onClick={toggleHelp}>
          <StyledIcon name="help" $bsStyle={bsStyle} />
        </StyledButton>
      </Popover.Target>
      <StyledOverlay>{helpText}</StyledOverlay>
    </Popover>
  );
};

export default HelpPopoverButton;
