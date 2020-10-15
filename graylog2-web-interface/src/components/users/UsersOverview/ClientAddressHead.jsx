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
