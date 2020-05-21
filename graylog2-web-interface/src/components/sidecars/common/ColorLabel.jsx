import React from 'react';
import PropTypes from 'prop-types';
import styled, { withTheme } from 'styled-components';
import d3 from 'd3';

import { Label } from 'components/graylog';

const ColorLabelWrap = styled.span(({ size }) => {
  const fontSize = size === 'small' ? '0.75em' : '1em';

  return `
    vertical-align: middle;
    font-size: ${size === 'xsmall' ? '0.5em' : fontSize};
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
    color: PropTypes.object,
  }).isRequired,
};

ColorLabel.defaultProps = {
  text: <span>&emsp;</span>,
  size: 'normal',
};

export default withTheme(ColorLabel);
