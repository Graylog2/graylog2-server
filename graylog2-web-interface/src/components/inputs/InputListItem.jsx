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
import PropTypes from 'prop-types';
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { LinkContainer } from 'components/common/router';
import { DropdownButton, MenuItem, Col, Button } from 'components/bootstrap';
import { EntityListItem, IfPermitted, LinkToNode, Spinner } from 'components/common';
import { ConfigurationWell } from 'components/configurationforms';
import PermissionsMixin from 'util/PermissionsMixin';
import Routes from 'routing/Routes';
import { InputForm, InputStateBadge, InputStateControl, InputStaticFields, InputThroughput, StaticFieldForm } from 'components/inputs';
import { InputsActions } from 'stores/inputs/InputsStore';
import { InputTypesStore } from 'stores/inputs/InputTypesStore';

const InputListItem = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  displayName: 'InputListItem',

  // eslint-disable-next-line react/no-unused-class-component-methods
  propTypes: {
    input: PropTypes.object.isRequired,
    currentNode: PropTypes.object.isRequired,
    permissions: PropTypes.array.isRequired,
  },

  mixins: [PermissionsMixin, Reflux.connect(InputTypesStore)],

  _deleteInput() {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to delete input '${this.props.input.title}'?`)) {
      InputsActions.delete(this.props.input);
    }
  },

  _openStaticFieldForm() {
    this.staticFieldForm.open();
  },

  _editInput() {
    this.configurationForm.open();
  },

  _updateInput(data) {
    InputsActions.update(this.props.input.id, data);
  },

  render() {
    if (!this.state.inputTypes) {
      return <Spinner />;
    }

    const { input } = this.props;
    const definition = this.state.inputDescriptions[input.type];

    const titleSuffix = (
      <span>
        {this.props.input.name}
        &nbsp;
        <InputStateBadge input={this.props.input} />
      </span>
    );

    const actions = [];

    if (this.isPermitted(this.props.permissions, ['searches:relative'])) {
      actions.push(
        <LinkContainer key={`received-messages-${this.props.input.id}`}
                       to={Routes.search(`gl2_source_input:${this.props.input.id}`, { relative: 0 })}>
          <Button>Show received messages</Button>
        </LinkContainer>,
      );
    }

    if (this.isPermitted(this.props.permissions, [`inputs:edit:${this.props.input.id}`])) {
      let extractorRoute;

      if (this.props.input.global) {
        extractorRoute = Routes.global_input_extractors(this.props.input.id);
      } else {
        extractorRoute = Routes.local_input_extractors(this.props.currentNode.node_id, this.props.input.id);
      }

      actions.push(
        <LinkContainer key={`manage-extractors-${this.props.input.id}`} to={extractorRoute}>
          <Button>Manage extractors</Button>
        </LinkContainer>,
      );

      actions.push(<InputStateControl key={`input-state-control-${this.props.input.id}`} input={this.props.input} />);
    }

    let showMetricsMenuItem;

    if (!this.props.input.global) {
      showMetricsMenuItem = (
        <LinkContainer to={Routes.filtered_metrics(this.props.input.node, this.props.input.id)}>
          <MenuItem key={`show-metrics-${this.props.input.id}`}>Show metrics</MenuItem>
        </LinkContainer>
      );
    }

    actions.push(
      <DropdownButton key={`more-actions-${this.props.input.id}`}
                      title="More actions"
                      id={`more-actions-dropdown-${this.props.input.id}`}
                      pullRight>
        <IfPermitted permissions={`inputs:edit:${this.props.input.id}`}>
          <MenuItem key={`edit-input-${this.props.input.id}`}
                    onSelect={this._editInput}
                    disabled={definition === undefined}>
            Edit input
          </MenuItem>
        </IfPermitted>

        {showMetricsMenuItem}

        <IfPermitted permissions={`inputs:edit:${this.props.input.id}`}>
          <MenuItem key={`add-static-field-${this.props.input.id}`} onSelect={this._openStaticFieldForm}>Add static field</MenuItem>
        </IfPermitted>

        <IfPermitted permissions="inputs:terminate">
          <MenuItem key={`divider-${this.props.input.id}`} divider />
        </IfPermitted>
        <IfPermitted permissions="inputs:terminate">
          <MenuItem key={`delete-input-${this.props.input.id}`} onSelect={this._deleteInput}>Delete input</MenuItem>
        </IfPermitted>
      </DropdownButton>,
    );

    let subtitle;

    if (!this.props.input.global && this.props.input.node) {
      subtitle = (
        <span>
          On node{' '}<LinkToNode nodeId={this.props.input.node} />
        </span>
      );
    }

    const inputForm = definition
      ? (
        <InputForm ref={(configurationForm) => { this.configurationForm = configurationForm; }}
                   key={`edit-form-input-${input.id}`}
                   globalValue={input.global}
                   nodeValue={input.node}
                   configFields={definition.requested_configuration}
                   title={`Editing Input ${input.title}`}
                   titleValue={input.title}
                   typeName={input.type}
                   includeTitleField
                   submitAction={this._updateInput}
                   submitButtonText="Update input"
                   values={input.attributes} />
      ) : null;

    const additionalContent = (
      <div>
        <Col md={8}>
          <ConfigurationWell className="configuration-well"
                             id={input.id}
                             configuration={input.attributes}
                             typeDefinition={definition || {}} />
          <StaticFieldForm ref={(staticFieldForm) => { this.staticFieldForm = staticFieldForm; }} input={this.props.input} />
          <InputStaticFields input={this.props.input} />
        </Col>
        <Col md={4}>
          <InputThroughput input={input} />
        </Col>
        {inputForm}
      </div>
    );

    return (
      <EntityListItem key={`entry-list-${this.props.input.id}`}
                      title={this.props.input.title}
                      titleSuffix={titleSuffix}
                      description={subtitle}
                      createdFromContentPack={!!this.props.input.content_pack}
                      actions={actions}
                      contentRow={additionalContent} />
    );
  },
});

export default InputListItem;
