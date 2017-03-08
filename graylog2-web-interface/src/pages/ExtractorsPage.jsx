import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import { DropdownButton, MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import PageHeader from 'components/common/PageHeader';
import ExtractorsList from 'components/extractors/ExtractorsList';
import { DocumentTitle, Spinner } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';

import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';

import ActionsProvider from 'injection/ActionsProvider';
const NodesActions = ActionsProvider.getActions('Nodes');
const InputsActions = ActionsProvider.getActions('Inputs');

import StoreProvider from 'injection/StoreProvider';
const NodesStore = StoreProvider.getStore('Nodes');
const InputsStore = StoreProvider.getStore('Inputs');

const ExtractorsPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(InputsStore), Reflux.listenTo(NodesStore, 'onNodesChange')],
  getInitialState() {
    return {
      input: undefined,
      node: undefined,
    };
  },
  componentDidMount() {
    InputsActions.get.triggerPromise(this.props.params.inputId);
    NodesActions.list.triggerPromise();
  },
  onNodesChange(nodes) {
    let inputNode;
    if (this.props.params.nodeId) {
      inputNode = nodes.nodes[this.props.params.nodeId];
    } else {
      const nodeIds = Object.keys(nodes.nodes);
      for (let i = 0; i < nodeIds.length && !inputNode; i++) {
        const tempNode = nodes.nodes[nodeIds[i]];
        if (tempNode.is_master) {
          inputNode = tempNode;
        }
      }
    }

    if (!this.state.node || this.state.node.node_id !== inputNode.node_id) {
      this.setState({ node: inputNode });
    }
  },
  _isLoading() {
    return !(this.state.input && this.state.node);
  },
  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    return (
      <DocumentTitle title={`Extractors of ${this.state.input.title}`}>
        <div>
          <PageHeader title={<span>Extractors of <em>{this.state.input.title}</em></span>}>
            <span>
              Extractors are applied on every message that is received by this input. Use them to extract and transform{' '}
              any text data into fields that allow you easy filtering and analysis later on.{' '}
              Example: Extract the HTTP response code from a log message, transform it to a numeric field and attach it{' '}
              as <em>http_response_code</em> to the message.
            </span>

            <span>
              Find more information about extractors in the
              {' '}<DocumentationLink page={DocsHelper.PAGES.EXTRACTORS} text="documentation" />.
            </span>

            <DropdownButton bsStyle="info" bsSize="large" id="extractor-actions-dropdown" title="Actions" pullRight>
              <LinkContainer to={Routes.import_extractors(this.state.node.node_id, this.state.input.id)}>
                <MenuItem>Import extractors</MenuItem>
              </LinkContainer>
              <LinkContainer to={Routes.export_extractors(this.state.node.node_id, this.state.input.id)}>
                <MenuItem>Export extractors</MenuItem>
              </LinkContainer>
            </DropdownButton>
          </PageHeader>
          <ExtractorsList input={this.state.input} node={this.state.node} />
        </div>
      </DocumentTitle>
    );
  },
});

export default ExtractorsPage;
