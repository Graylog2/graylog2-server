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
import React, { useState } from 'react';
import styled from 'styled-components';

import { Button } from 'components/bootstrap';
import { Icon } from 'components/common';
import Popover from 'components/common/Popover';

type Props = {
  helpText: string;
};

const StyledButton = styled(Button)`
  padding: 1px 0;
`;

const DiagnosisHelp = ({ helpText }: Props) => {
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
        <StyledButton bsStyle="transparent" bsSize="xs" onClick={toggleHelp}>
          <Icon name="question_mark" />
        </StyledButton>
      </Popover.Target>
      <Popover.Dropdown>{helpText}</Popover.Dropdown>
    </Popover>
  );
};

export default DiagnosisHelp;
