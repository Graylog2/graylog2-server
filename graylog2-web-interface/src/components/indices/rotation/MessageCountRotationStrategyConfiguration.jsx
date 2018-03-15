import PropTypes from 'prop-types';
import React from 'react';
import { Input } from 'components/bootstrap';

class MessageCountRotationStrategyConfiguration extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    jsonSchema: PropTypes.object.isRequired,
    updateConfig: PropTypes.func.isRequired,
  };

  state = {
    max_docs_per_index: this.props.config.max_docs_per_index,
  };

  _onInputUpdate = (field) => {
    return (e) => {
      const update = {};
      update[field] = e.target.value;

      this.setState(update);
      this.props.updateConfig(update);
    };
  };

  render() {
    return (
      <div>
        <fieldset>

          <Input type="number"
                 id="max-docs-per-index"
                 label="Max documents per index"
                 onChange={this._onInputUpdate('max_docs_per_index')}
                 value={this.state.max_docs_per_index}
                 help="Maximum number of documents in an index before it gets rotated"
                 required />
        </fieldset>
      </div>
    );
  }
}

export default MessageCountRotationStrategyConfiguration;
