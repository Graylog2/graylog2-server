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
// @flow strict
import * as React from 'react';
import styled from 'styled-components';

import { OverlayTrigger, Popover, Button } from 'components/graylog';
import { Icon } from 'components/common';

type Props = {
  title: string,
};

const TooltipButton = styled(Button)`
  cursor: help;
  padding: 0 0 0 2px;
  display: inline-flex;
`;

const ClientAddressHead = ({ title }: Props) => {
  const popover = (
    <Popover id="client-address-help">
      <p>
        The address of the client used to initially establish the session, not necessarily its current address.
      </p>
    </Popover>
  );

  return (
    <th>
      {title}
      <OverlayTrigger trigger="click" rootClose placement="top" overlay={popover}>
        <TooltipButton bsStyle="link">
          <Icon name="question-circle" fixedWidth />
        </TooltipButton>
      </OverlayTrigger>
    </th>
  );
};

export default ClientAddressHead;
