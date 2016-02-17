import React from 'react';
import { Input } from 'react-bootstrap';

const DeletionRetentionStrategyConfiguration = React.createClass({
  propTypes: {
    config: React.PropTypes.object.isRequired,
    jsonSchema: React.PropTypes.object.isRequired,
    updateConfig: React.PropTypes.func.isRequired,
  },

  getInitialState() {
    return {
      max_number_of_indices: this.props.config.max_number_of_indices,
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

  render() {
    return (
      <div>
        <fieldset>
          <Input type="number"
                 id="max-number-of-indices"
                 label="Max number of indices"
                 onChange={this._onInputUpdate('max_number_of_indices')}
                 value={this.state.max_number_of_indices}
                 help={<span>Maximum number of indices to keep before <strong>deleting</strong> the oldest ones</span>}
                 autoFocus
                 required />
        </fieldset>
      </div>
    );
  },
});

export default DeletionRetentionStrategyConfiguration;
