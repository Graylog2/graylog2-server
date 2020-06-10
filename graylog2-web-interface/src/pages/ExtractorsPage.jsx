import { DocumentTitle, Spinner } from 'components/common';

import PageHeader from 'components/common/PageHeader';
import ExtractorsList from 'components/extractors/ExtractorsList';
import DocumentationLink from 'components/support/DocumentationLink';
import createReactClass from 'create-react-class';

import ActionsProvider from 'injection/ActionsProvider';

import StoreProvider from 'injection/StoreProvider';
import PropTypes from 'prop-types';
import React from 'react';
import { DropdownButton, MenuItem } from 'components/graylog';
import { LinkContainer } from 'react-router-bootstrap';
import Reflux from 'reflux';

import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';

const NodesActions = ActionsProvider.getActions('Nodes');
const InputsActions = ActionsProvider.getActions('Inputs');
const NodesStore = StoreProvider.getStore('Nodes');

const ExtractorsPage = createReactClass({
  displayName: 'ExtractorsPage',

  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [Reflux.listenTo(NodesStore, 'onNodesChange')],

  getInitialState() {
    return {
      node: undefined,
    };
  },

  componentDidMount() {
    const { params } = this.props;
    InputsActions.get(params.inputId).then((input) => this.setState({ input }));
    NodesActions.list();
  },

  onNodesChange(nodes) {
    const { params } = this.props;
    const newNode = params.nodeId ? nodes.nodes[params.nodeId] : Object.values(nodes.nodes).filter((node) => node.is_primary);

    const { node } = this.state;
    if (!node || node.node_id !== newNode.node_id) {
      this.setState({ node: newNode });
    }
  },

  _isLoading() {
    const { node, input } = this.state;
    return !(input && node);
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { node, input } = this.state;
    return (
      <DocumentTitle title={`Extractors of ${input.title}`}>
        <div>
          <PageHeader title={<span>Extractors of <em>{input.title}</em></span>}>
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
              <LinkContainer to={Routes.import_extractors(node.node_id, input.id)}>
                <MenuItem>Import extractors</MenuItem>
              </LinkContainer>
              <LinkContainer to={Routes.export_extractors(node.node_id, input.id)}>
                <MenuItem>Export extractors</MenuItem>
              </LinkContainer>
            </DropdownButton>
          </PageHeader>
          <ExtractorsList input={input} node={node} />
        </div>
      </DocumentTitle>
    );
  },
});

export default ExtractorsPage;
