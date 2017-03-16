import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';
import naturalSort from 'javascript-natural-sort';

import EntityList from 'components/common/EntityList';
import InputListItem from './InputListItem';
import { IfPermitted, Spinner } from 'components/common';
import CreateInputControl from './CreateInputControl';

import ActionsProvider from 'injection/ActionsProvider';
const InputsActions = ActionsProvider.getActions('Inputs');
const SingleNodeActions = ActionsProvider.getActions('SingleNode');
const InputTypesActions = ActionsProvider.getActions('InputTypes');

import StoreProvider from 'injection/StoreProvider';
const InputsStore = StoreProvider.getStore('Inputs');
const SingleNodeStore = StoreProvider.getStore('SingleNode');

const InputsList = React.createClass({
  propTypes: {
    permissions: PropTypes.array.isRequired,
    node: PropTypes.object,
  },
  mixins: [Reflux.connect(SingleNodeStore), Reflux.listenTo(InputsStore, '_splitInputs')],
  getInitialState() {
    return {
      globalInputs: undefined,
      localInputs: undefined,
    };
  },
  componentDidMount() {
    InputTypesActions.list();
    InputsActions.list();
    SingleNodeActions.get();
  },
  _splitInputs(state) {
    const inputs = state.inputs;
    const globalInputs = inputs
      .filter(input => input.global === true)
      .sort((inputA, inputB) => naturalSort(inputA.title, inputB.title));
    let localInputs = inputs
      .filter(input => input.global === false)
      .sort((inputA, inputB) => naturalSort(inputA.title, inputB.title));

    if (this.props.node) {
      localInputs = localInputs.filter(input => input.node === this.props.node.node_id);
    }

    this.setState({ globalInputs: globalInputs, localInputs: localInputs });
  },
  _isLoading() {
    return !(this.state.localInputs && this.state.globalInputs && this.state.node);
  },
  _formatInput(input) {
    return <InputListItem key={input.id} input={input} currentNode={this.state.node} permissions={this.props.permissions} />;
  },
  _nodeAffix() {
    return (this.props.node ? ' on this node' : '');
  },
  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    return (
      <div>
        {!this.props.node &&
        <IfPermitted permissions="inputs:create">
          <CreateInputControl />
        </IfPermitted>
        }

        <Row id="global-inputs" className="content input-list">
          <Col md={12}>
            <h2>
              Global inputs
              &nbsp;
              <small>{this.state.globalInputs.length} configured{this._nodeAffix()}</small>
            </h2>
            <EntityList bsNoItemsStyle="info" noItemsText="There are no global inputs."
                        items={this.state.globalInputs.map(input => this._formatInput(input))} />
          </Col>
        </Row>
        <Row id="local-inputs" className="content input-list">
          <Col md={12}>
            <h2>
              Local inputs
              &nbsp;
              <small>{this.state.localInputs.length} configured{this._nodeAffix()}</small>
            </h2>
            <EntityList bsNoItemsStyle="info" noItemsText={`There are no local inputs${this._nodeAffix()}.`}
                        items={this.state.localInputs.map(input => this._formatInput(input))} />
          </Col>
        </Row>
      </div>
    );
  },
});

export default InputsList;
