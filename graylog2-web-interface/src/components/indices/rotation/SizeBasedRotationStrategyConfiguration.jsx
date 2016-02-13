import React from 'react';
import { Input } from 'react-bootstrap';

import numeral from 'numeral';

const SizeBasedRotationStrategyConfiguration = React.createClass({
  propTypes: {
    config: React.PropTypes.object.isRequired,
    jsonSchema: React.PropTypes.object.isRequired,
    updateConfig: React.PropTypes.func.isRequired,
  },

  getInitialState() {
    return {
      max_size: this.props.config.max_size,
    }
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
    return numeral(this.state.max_size).format('0.0b');
  },

  render() {
    return (
      <div>
        <fieldset>
          <Input type='number'
                 id='max-size'
                 label='Max size per index (in bytes)'
                 onChange={this._onInputUpdate('max_size')}
                 value={this.state.max_size}
                 help='Maximum size of an index before it gets rotated'
                 addonAfter={this._formatSize()}
                 autoFocus
                 required />
        </fieldset>
      </div>
    );
  },
});

export default SizeBasedRotationStrategyConfiguration;
