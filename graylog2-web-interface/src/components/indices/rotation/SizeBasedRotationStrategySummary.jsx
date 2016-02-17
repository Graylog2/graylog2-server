import React from 'react';

import numeral from 'numeral';
import style from '!style!css!../IndicesConfiguration.css';

const SizeBasedRotationStrategySummary = React.createClass({
  propTypes: {
    config: React.PropTypes.object.isRequired,
  },

  render() {
    return (
      <div>
        <dl className={style.deflist}>
          <dt>Index rotation strategy:</dt>
          <dd>Index Size</dd>
          <dt>Max index size:</dt>
          <dd>{this.props.config.max_size} bytes ({numeral(this.props.config.max_size).format('0.0b')})</dd>
        </dl>
      </div>
    );
  },
});

export default SizeBasedRotationStrategySummary;
