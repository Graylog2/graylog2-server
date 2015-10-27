import React, {PropTypes} from 'react';
import Reflux from 'reflux';

import EntityList from 'components/common/EntityList';
import InputListItem from './InputListItem';
import Spinner from 'components/common/Spinner';
import InputsStore from 'stores/inputs/InputsStore';
import SingleNodeActions from 'actions/nodes/SingleNodeActions';
import SingleNodeStore from 'stores/nodes/SingleNodeStore';

const InputsList = React.createClass({
  propTypes: {
    permissions: PropTypes.array.isRequired,
  },
  mixins: [Reflux.connect(SingleNodeStore), Reflux.ListenerMethods],
  getInitialState() {
    return {
      globalInputs: undefined,
      localInputs: undefined,
    };
  },
  componentDidMount() {
    InputsStore.list(inputs => {
      const globalInputs = inputs.filter((input) => input.message_input.global === true);
      const localInputs = inputs.filter((input) => input.message_input.global === false);
      this.setState({globalInputs: globalInputs, localInputs: localInputs});
    }, false);
    SingleNodeActions.get.triggerPromise();
  },
  _isLoading() {
    return !(this.state.localInputs && this.state.globalInputs && this.state.node);
  },
  _formatInput(input) {
    return <InputListItem key={input.id} input={input} currentNode={this.state.node} permissions={this.props.permissions}/>;
  },
  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }

    let globalInputs;

    if (this.state.globalInputs) {
      globalInputs = <div>Global inputs go here!</div>;
    } else {
      globalInputs = <div className="alert alert-info" role="alert">No global inputs running.</div>;
    }

    return (
      <div>
        <div className="row content">Cannot create inputs at the moment</div>
        <div className="row content input-list">
          <div className="col-md-12">
            <h2>
              Global inputs
              &nbsp;
              <small>{this.state.globalInputs.length} configured on this node</small>
            </h2>
            {globalInputs}
          </div>
        </div>
        <div className="row content input-list">
          <div className="col-md-12">
            <h2>
              Local inputs
              &nbsp;
              <small>{this.state.localInputs.length} configured on this node</small>
            </h2>
            <EntityList bsNoItemsStyle="info" noItemsText="There are no local inputs."
                        items={this.state.localInputs.map(input => this._formatInput(input))} />
          </div>
        </div>
      </div>
    );
  },
});

export default InputsList;
