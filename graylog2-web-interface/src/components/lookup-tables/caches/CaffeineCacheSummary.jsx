import PropTypes from 'prop-types';
import React from 'react';
import { TimeUnit } from 'components/common';

class CaffeineCacheSummary extends React.Component {
  static propTypes = {
    cache: PropTypes.object.isRequired,
  };

  render() {
    const { config } = this.props.cache;
    return (
      <dl>
        <dt>Maximum entries</dt>
        <dd>{config.max_size}</dd>
        <dt>Expire after access</dt>
        <dd><TimeUnit value={config.expire_after_access} unit={config.expire_after_access_unit} /></dd>
        <dt>Expire after write</dt>
        <dd><TimeUnit value={config.expire_after_write} unit={config.expire_after_write_unit} /></dd>
      </dl>
    );
  }
}

export default CaffeineCacheSummary;
