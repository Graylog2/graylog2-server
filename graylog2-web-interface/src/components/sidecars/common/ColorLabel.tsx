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
import PropTypes from 'prop-types';
import styled, { css, withTheme, DefaultTheme } from 'styled-components';

import { themePropTypes } from 'theme';
import { Label } from 'components/graylog';

type Size = 'normal' | 'small' | 'xsmall';

interface ColorLabelWrapProps {
  size: Size,
  theme: DefaultTheme
}

interface ColorLabelProps {
  color: string,
  size?: Size,
  text?: string | React.ReactNode,
  theme: DefaultTheme
}

const ColorLabelWrap = styled.span(({ size, theme }: ColorLabelWrapProps) => {
  const { body, small, tiny } = theme.fonts.size;
  const fontSize = size === 'small' ? small : body;

  return css`
    vertical-align: middle;
    font-size: ${size === 'xsmall' ? tiny : fontSize};
  `;
});

const ColorLabel = ({ color, size, text, theme }: ColorLabelProps) => {
  const borderColor = theme.utils.colorLevel(color, 5);
  const textColor = theme.utils.contrastingColor(color);

  return (
    <ColorLabelWrap size={size}>
      <Label style={{
        backgroundColor: color,
        border: `1px solid ${borderColor}`,
        color: textColor,
      }}>
        {text}
      </Label>
    </ColorLabelWrap>
  );
};

ColorLabel.propTypes = {
  color: PropTypes.string.isRequired,
  text: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
  size: PropTypes.oneOf(['normal', 'small', 'xsmall']),
  theme: themePropTypes.isRequired,
};

ColorLabel.defaultProps = {
  text: <span>&emsp;</span>,
  size: 'normal',
};

export default withTheme(ColorLabel);
