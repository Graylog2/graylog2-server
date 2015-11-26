import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import {Row, Col} from 'react-bootstrap';

import EntityList from 'components/common/EntityList';
import InputListItem from './InputListItem';
import Spinner from 'components/common/Spinner';

import InputsActions from 'actions/inputs/InputsActions';
import InputsStore from 'stores/inputs/InputsStore';
import SingleNodeActions from 'actions/nodes/SingleNodeActions';
import SingleNodeStore from 'stores/nodes/SingleNodeStore';

const InputsList = React.createClass({
  propTypes: {
    permissions: PropTypes.array.isRequired,
  },
  mixins: [Reflux.connect(SingleNodeStore), Reflux.listenTo(InputsStore, '_splitInputs')],
  getInitialState() {
    return {
      globalInputs: undefined,
      localInputs: undefined,
    };
  },
  componentDidMount() {
    InputsActions.list.triggerPromise(true);
    SingleNodeActions.get.triggerPromise();
  },
  _splitInputs(state) {
    const inputs = state.inputs;
    const globalInputs = inputs.filter((input) => input.message_input.global === true);
    const localInputs = inputs.filter((input) => input.message_input.global === false);
    this.setState({globalInputs: globalInputs, localInputs: localInputs});
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

    return (
      <div>
        <Row className="content">Cannot create inputs at the moment</Row>
        <Row className="content input-list">
          <Col md={12}>
            <h2>
              Global inputs
              &nbsp;
              <small>{this.state.globalInputs.length} configured on this node</small>
            </h2>
            <EntityList bsNoItemsStyle="info" noItemsText="There are no global inputs."
                        items={this.state.globalInputs.map(input => this._formatInput(input))} />
          </Col>
        </Row>
        <Row className="content input-list">
          <Col md={12}>
            <h2>
              Local inputs
              &nbsp;
              <small>{this.state.localInputs.length} configured on this node</small>
            </h2>
            <EntityList bsNoItemsStyle="info" noItemsText="There are no local inputs."
                        items={this.state.localInputs.map(input => this._formatInput(input))} />
          </Col>
        </Row>
      </div>
    );
  },
});

export default InputsList;
