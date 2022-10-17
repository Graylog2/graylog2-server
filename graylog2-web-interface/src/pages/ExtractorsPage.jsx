/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import React from 'react';
import Reflux from 'reflux';

import { LinkContainer } from 'components/common/router';
import { DocumentTitle, Spinner } from 'components/common';
import PageHeader from 'components/common/PageHeader';
import ExtractorsList from 'components/extractors/ExtractorsList';
import { DropdownButton, MenuItem } from 'components/bootstrap';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import withParams from 'routing/withParams';
import { InputsActions } from 'stores/inputs/InputsStore';
import { NodesActions, NodesStore } from 'stores/nodes/NodesStore';

const ExtractorsPage = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  displayName: 'ExtractorsPage',

  // eslint-disable-next-line react/no-unused-class-component-methods
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

  // eslint-disable-next-line react/no-unused-class-component-methods
  onNodesChange(nodes) {
    const { params } = this.props;
    const newNode = params.nodeId ? nodes.nodes[params.nodeId] : Object.values(nodes.nodes).filter((node) => node.is_leader);

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
          <PageHeader title={<span>Extractors of <em>{input.title}</em></span>}
                      subactions={(
                        <DropdownButton bsStyle="info" id="extractor-actions-dropdown" title="Actions" pullRight>
                          <LinkContainer to={Routes.import_extractors(node.node_id, input.id)}>
                            <MenuItem>Import extractors</MenuItem>
                          </LinkContainer>
                          <LinkContainer to={Routes.export_extractors(node.node_id, input.id)}>
                            <MenuItem>Export extractors</MenuItem>
                          </LinkContainer>
                        </DropdownButton>
                      )}
                      documentationLink={{
                        title: 'Extractors documentation',
                        path: DocsHelper.PAGES.EXTRACTORS,
                      }}>
            <span>
              Extractors are applied on every message that is received by this input. Use them to extract and transform{' '}
              any text data into fields that allow you easy filtering and analysis later on.{' '}
              Example: Extract the HTTP response code from a log message, transform it to a numeric field and attach it{' '}
              as <em>http_response_code</em> to the message.
            </span>

          </PageHeader>
          <ExtractorsList input={input} node={node} />
        </div>
      </DocumentTitle>
    );
  },
});

export default withParams(ExtractorsPage);
