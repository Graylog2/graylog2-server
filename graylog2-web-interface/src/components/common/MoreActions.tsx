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
import styled from 'styled-components';

import { Icon } from 'components/common/index';
import { DropdownButton } from 'components/bootstrap';
import type { StyleProps } from 'components/bootstrap/Button';
import type { SizeProp } from 'components/common/Icon';

type MoreActionsIconProps = {
  size?: SizeProp;
};
export const MoreActionsIcon = ({ size = undefined }: MoreActionsIconProps) => <Icon name="more_horiz" size={size} />;

type MoreActionsMenuProps = {
  'aria-label'?: string;
  size?: SizeProp;
  bsStyle?: StyleProps;
  className?: string;
  id?: string;
  keepMounted?: boolean;
  pullRight?: boolean;
  title?: string;
  solid?: boolean;
};
const StyledDropdownButton = styled(DropdownButton)<{ $transparent?: boolean }>`
  ${({ $transparent }) => ($transparent ? 'background-color: transparent;' : '')}
`;
export const MoreActionsMenu = ({
  'aria-label': ariaLabel,
  size = 'xs',
  bsStyle = undefined,
  children = undefined,
  className = undefined,
  id = undefined,
  keepMounted = undefined,
  pullRight = undefined,
  title = 'More Actions',
  solid = false,
}: React.PropsWithChildren<MoreActionsMenuProps>) => (
  <StyledDropdownButton
    $transparent={!solid}
    aria-label={ariaLabel}
    bsStyle={bsStyle}
    buttonTitle={title}
    className={className}
    id={id}
    keepMounted={keepMounted}
    noCaret
    pullRight={pullRight}
    title={<MoreActionsIcon size={size} />}
    withinPortal>
    {children}
  </StyledDropdownButton>
);
