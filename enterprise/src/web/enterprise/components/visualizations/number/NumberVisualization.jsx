import React from 'react';
import PropTypes from 'prop-types';

import NumberUtils from 'util/NumberUtils';
import style from 'components/visualizations/NumericVisualization.css';

const DEFAULT_VALUE_FONT_SIZE = '60px';

const _formatData = data => String(NumberUtils.formatNumber(data));

const _calculateFontSize = (data) => {
  if (typeof data === 'undefined') {
    return DEFAULT_VALUE_FONT_SIZE;
  }

  let fontSize;
  const formattedLength = _formatData(data).length;

  if (formattedLength < 7) {
    fontSize = DEFAULT_VALUE_FONT_SIZE;
  } else {
    switch (formattedLength) {
      case 7:
        fontSize = '50px';
        break;
      case 8:
        fontSize = '45px';
        break;
      case 9:
      case 10:
        fontSize = '40px';
        break;
      case 11:
      case 12:
        fontSize = '35px';
        break;
      default:
        fontSize = '25px';
    }
  }

  return fontSize;
};

const _extractValue = (data) => {
  if (!data || !data[0]) {
    return undefined;
  }
  const results = data[0];
  const leaf = results.values.find(f => f.source === 'row-leaf');
  return leaf ? leaf.value : undefined;
};

const NumberVisualization = ({ data, height, width }) => {
  const value = _extractValue(data);
  return (
    <div className={style.container}>
      <svg viewBox="0 0 300 100"
           className={style.number}
           width="100%"
           height="100%"
           style={{ height: height, width: width }}>
        <text x="150" y="45" className={style.value} style={{ fontSize: _calculateFontSize(value) }}>
          {_formatData(value)}
        </text>
      </svg>
    </div>
  );
};

NumberVisualization.propTypes = {
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
  height: PropTypes.number.isRequired,
  width: PropTypes.number.isRequired,
};
NumberVisualization.type = 'numeric';

export default NumberVisualization;
