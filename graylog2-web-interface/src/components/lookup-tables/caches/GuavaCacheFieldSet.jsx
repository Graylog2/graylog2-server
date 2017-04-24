import React, { PropTypes } from 'react';

import { Input } from 'components/bootstrap';

const NullCacheFieldSet = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  },

  render() {
    const config = this.props.config;

    return (<fieldset>
      <Input type="text"
             id="max_size"
             name="max_size"
             label="Maximum entries"
             autoFocus
             required
             onChange={this.props.onChange}
             help="The limit of the number of entries the cache keeps in memory."
             value={config.max_size}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
    </fieldset>);
  },
});

export default NullCacheFieldSet;
