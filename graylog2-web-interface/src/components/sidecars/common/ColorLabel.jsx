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
import styled, { withTheme } from 'styled-components';
import d3 from 'd3';

import { Label } from 'components/graylog';

const ColorLabelWrap = styled.span(({ size, theme }) => {
  const { body, small, tiny } = theme.fonts.size;
  const fontSize = size === 'small' ? small : body;

  return `
    vertical-align: middle;
    font-size: ${size === 'xsmall' ? tiny : fontSize};
  `;
});

const ColorLabel = ({ color, size, text, theme }) => {
  const backgroundColor = d3.hsl(color);
  const borderColor = backgroundColor.darker();
  // Use dark font on brighter backgrounds and light font in darker backgrounds
  const textColor = backgroundColor.l > 0.6 ? d3.rgb(theme.colors.global.textDefault) : d3.rgb(theme.colors.global.textAlt);

  return (
    <ColorLabelWrap size={size}>
      <Label style={{
        backgroundColor: backgroundColor.toString(),
        border: `1px solid ${borderColor.toString()}`,
        color: textColor.toString(),
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
  theme: PropTypes.shape({
    colors: PropTypes.object,
  }).isRequired,
};

ColorLabel.defaultProps = {
  text: <span>&emsp;</span>,
  size: 'normal',
};

export default withTheme(ColorLabel);
