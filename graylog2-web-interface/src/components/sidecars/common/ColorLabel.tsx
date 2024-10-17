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
import styled, { css, useTheme } from 'styled-components';

import { Label } from 'components/bootstrap';

type Size = 'normal' | 'small' | 'xsmall';

const ColorLabelWrap = styled.span<{ $size: Size }>(({ $size, theme }) => {
  const { body, small, tiny } = theme.fonts.size;
  const fontSize = $size === 'small' ? small : body;

  return css`
    vertical-align: middle;
    font-size: ${$size === 'xsmall' ? tiny : fontSize};
`;
});

type Props = {
  color: string,
  size?: Size,
  text?: string | React.ReactNode,
}

const ColorLabel = ({ color, size = 'normal', text = <span>&emsp;</span> }: Props) => {
  const theme = useTheme();
  const borderColor = theme.utils.colorLevel(color, 5);
  const textColor = theme.utils.contrastingColor(color);

  return (
    <ColorLabelWrap $size={size} className="color-label-wrapper">
      <Label style={{
        backgroundColor: color,
        border: `1px solid ${borderColor}`,
        color: textColor,
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        maxWidth: '128px',
        marginRight: '4px',
        marginBottom: '4px',
      }}>
        {text}
      </Label>
    </ColorLabelWrap>
  );
};

export default ColorLabel;
