import React from 'react';
import { Input } from 'components/bootstrap';

import NumberUtils from 'util/NumberUtils';

const SizeBasedRotationStrategyConfiguration = React.createClass({
  propTypes: {
    config: React.PropTypes.object.isRequired,
    jsonSchema: React.PropTypes.object.isRequired,
    updateConfig: React.PropTypes.func.isRequired,
  },

  getInitialState() {
    return {
      max_size: this.props.config.max_size,
    };
  },

  _onInputUpdate(field) {
    return (e) => {
      const update = {};
      update[field] = e.target.value;

      this.setState(update);
      this.props.updateConfig(update);
    };
  },

  _formatSize() {
    return NumberUtils.formatBytes(this.state.max_size);
  },

  render() {
    return (
      <div>
        <fieldset>
          <Input type="number"
                 id="max-size"
                 label="Max size per index (in bytes)"
                 onChange={this._onInputUpdate('max_size')}
                 value={this.state.max_size}
                 help="Maximum size of an index before it gets rotated"
                 addonAfter={this._formatSize()}
                 required />
        </fieldset>
      </div>
    );
  },
});

export default SizeBasedRotationStrategyConfiguration;
