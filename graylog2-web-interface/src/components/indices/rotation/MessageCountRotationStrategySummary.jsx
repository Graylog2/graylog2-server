import React from 'react';

import style from '!style!css!../IndicesConfiguration.css';

const MessageCountRotationStrategySummary = React.createClass({
  propTypes: {
    config: React.PropTypes.object.isRequired,
  },

  render() {
    return (
      <div>
        <dl className={style.deflist}>
          <dt>Index rotation strategy:</dt>
          <dd>Message Count</dd>
          <dt>Max docs per index:</dt>
          <dd>{this.props.config.max_docs_per_index}</dd>
        </dl>
      </div>
    );
  },
});

export default MessageCountRotationStrategySummary;
