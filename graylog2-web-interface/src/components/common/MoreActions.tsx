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
  id?: string;
  'aria-label'?: string;
  bsStyle?: StyleProps;
  pullRight?: boolean;
  keepMounted?: boolean;
  className?: string;
  title?: string;
};
const StyledDropdownButton = styled(DropdownButton)`
  background-color: transparent;
`;
export const MoreActionsMenu = ({
  'aria-label': ariaLabel,
  bsStyle = undefined,
  className = undefined,
  children = undefined,
  id = undefined,
  pullRight = undefined,
  keepMounted = undefined,
  title = 'More Actions',
}: React.PropsWithChildren<MoreActionsMenuProps>) => (
  <StyledDropdownButton
    className={className}
    bsStyle={bsStyle}
    buttonTitle={title}
    title={<MoreActionsIcon />}
    noCaret
    bsSize="xs"
    id={id}
    aria-label={ariaLabel}
    pullRight={pullRight}
    keepMounted={keepMounted}>
    {children}
  </StyledDropdownButton>
);
