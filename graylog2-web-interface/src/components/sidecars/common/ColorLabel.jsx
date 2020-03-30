import React from 'react';
import PropTypes from 'prop-types';
import { Label } from 'components/graylog';
import { withTheme } from 'styled-components';
import d3 from 'd3';

import style from './ColorLabel.css';

class ColorLabel extends React.Component {
  static propTypes = {
    color: PropTypes.string.isRequired,
    text: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
    size: PropTypes.oneOf(['normal', 'small', 'xsmall']),
    theme: PropTypes.shape({
      color: PropTypes.object,
    }).isRequired,
  };

  static defaultProps = {
    // eslint-disable-next-line react/self-closing-comp
    text: <span>&emsp;</span>,
    size: 'normal',
  };

  render() {
    const { color, size, theme } = this.props;

    const backgroundColor = d3.hsl(color);
    const borderColor = backgroundColor.darker();
    // Use dark font on brighter backgrounds and light font in darker backgrounds
    const textColor = backgroundColor.l > 0.6 ? d3.rgb(theme.color.global.textDefault) : d3.rgb(theme.color.global.textAlt);
    return (
      <span className={`${style.colorLabel} ${style[size]}`}>
        <Label style={{
          backgroundColor: backgroundColor.toString(),
          border: `1px solid ${borderColor.toString()}`,
          color: textColor.toString(),
        }}>
          {this.props.text}
        </Label>
      </span>
    );
  }
}

export default withTheme(ColorLabel);
