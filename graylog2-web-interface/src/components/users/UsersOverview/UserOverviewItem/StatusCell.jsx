// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import UserOverview from 'logic/users/UserOverview';
import { OverlayTrigger, Popover } from 'components/graylog';
import type { ThemeInterface } from 'theme';
import { Icon } from 'components/common';

type Props = {
  accountStatus: $PropertyType<UserOverview, 'accountStatus'>,
};

const Wrapper: StyledComponent<{enabled?: boolean}, ThemeInterface, HTMLDivElement> = styled.div(({ theme, enabled }) => `
  color: ${enabled ? theme.colors.variant.success : theme.colors.variant.default};
`);

const Td: StyledComponent<{}, ThemeInterface, HTMLTableCellElement> = styled.td`
  width: 35px;
  text-align: center;
`;

const StatusCell = ({ accountStatus }: Props) => (
  <Td>
    <OverlayTrigger trigger={['hover', 'focus']}
                    placement="right"
                    overlay={(
                      <Popover id="session-badge-details">
                        {`User is ${accountStatus}`}
                      </Popover>
                    )}
                    rootClose>
      <Wrapper enabled={accountStatus === 'enabled'}>
        <Icon name={accountStatus === 'enabled' ? 'check-circle' : 'times-circle'} />
      </Wrapper>
    </OverlayTrigger>
  </Td>
);

export default StatusCell;
