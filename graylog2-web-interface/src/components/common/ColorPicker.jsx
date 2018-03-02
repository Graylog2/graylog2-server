import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { SwatchesPicker } from 'react-color';

/**
 * Color picker component that let the user select a color from a list of 95 colors grouped by hue.
 */
const ColorPicker = createReactClass({
  propTypes: {
    /** Indicates the selected color in hexadecimal format. */
    color: PropTypes.string,
    /**
     * Color palette in hexadecimal format. By default it uses the color palette defined by react-color,
     * including 95 colors to pick from.
     */
    colors: PropTypes.array,
    /**
     * Height of the color picker in pixels. By default it displays 2 rows of colors and the first color
     * of the third row, indicating users that they can scroll through the list:
     *
     * `135px height per color row * 2 rows + 24px first color of 3rd row + 16px padding = 310px`
     *
     * You can set `Infinity` as `height` if you don't want the component to scroll.
     */
    height: PropTypes.number,
    /**
     * Width of the color picker in pixels. By default it displays 5 columns of colors:
     *
     * `50px width per color column * 5 columns + 22px of padding = 272px`
     */
    width: PropTypes.number,
    /**
     * Function that will be called when the selected color changes.
     * The function receives the color in hexadecimal format as first
     * argument and the event as the second argument.
     */
    onChange: PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      color: undefined,
      colors: undefined, // Use default color palette.
      height: (135 * 2) + 24 + 16, // 135px color row * 2 rows + 24px first color 3rd row + 16px padding
      width: (50 * 5) + 16 + 6, // 50px color columns * 5 columns + 22px padding
    };
  },

  onColorChange(color, event) {
    this.props.onChange(color.hex, event);
  },

  render() {
    return (
      <SwatchesPicker {...this.props} onChange={this.onColorChange} />
    );
  },
});

export default ColorPicker;
