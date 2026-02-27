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
import React from 'react';
import styled, { css } from 'styled-components';

import { IconButton } from 'components/common';

const Title = styled.div(
  ({ theme }) => css`
    display: flex;
    gap: ${theme.spacings.xs};
    align-items: center;
    min-height: 25px; // as icon button
  `,
);

const StyledIconButton = styled(IconButton)(
  ({ theme }) => css`
    position: absolute;
    left: 0;
    color: ${theme.colors.text.primary};
  `,
);

const StyledLabel = styled.span(
  ({ theme }) => css`
    margin-left: ${theme.spacings.sm};
  `,
);

type Props = React.PropsWithChildren<{ onBackClick?: (() => void) | false }>;

const PopoverTitle = ({ children = null, onBackClick = false }: Props) => (
  <Title>
    {onBackClick && <StyledIconButton size="xs" name="arrow_back" title="Back" onClick={onBackClick} />}
    <StyledLabel>{children}</StyledLabel>
  </Title>
);

export default PopoverTitle;
