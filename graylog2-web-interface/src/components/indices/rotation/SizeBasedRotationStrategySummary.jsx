import React from 'react';
import NumberUtils from 'util/NumberUtils';

const SizeBasedRotationStrategySummary = React.createClass({
  propTypes: {
    config: React.PropTypes.object.isRequired,
  },

  render() {
    return (
      <div>
        <dl className="deflist">
          <dt>Index rotation strategy:</dt>
          <dd>Index Size</dd>
          <dt>Max index size:</dt>
          <dd>{this.props.config.max_size} bytes ({NumberUtils.formatBytes(this.props.config.max_size)})</dd>
        </dl>
      </div>
    );
  },
});

export default SizeBasedRotationStrategySummary;
