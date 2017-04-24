import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import { Button, DropdownButton, MenuItem, Col } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { EntityListItem, IfPermitted, LinkToNode, Spinner } from 'components/common';
import { ConfigurationWell } from 'components/configurationforms';

import PermissionsMixin from 'util/PermissionsMixin';
import Routes from 'routing/Routes';

import StoreProvider from 'injection/StoreProvider';
const InputTypesStore = StoreProvider.getStore('InputTypes');

import ActionsProvider from 'injection/ActionsProvider';
const InputsActions = ActionsProvider.getActions('Inputs');

import { InputForm, InputStateBadge, InputStateControl, InputStaticFields, InputThroughput, StaticFieldForm } from 'components/inputs';

const InputListItem = React.createClass({
  propTypes: {
    input: PropTypes.object.isRequired,
    currentNode: PropTypes.object.isRequired,
    permissions: PropTypes.array.isRequired,
  },
  mixins: [PermissionsMixin, Reflux.connect(InputTypesStore)],
  _deleteInput() {
    if (window.confirm(`Do you really want to delete input '${this.props.input.title}'?`)) {
      InputsActions.delete(this.props.input);
    }
  },

  _openStaticFieldForm() {
    this.refs.staticFieldForm.open();
  },
  _editInput() {
    this.refs.configurationForm.open();
  },
  _updateInput(data) {
    InputsActions.update(this.props.input.id, data);
  },
  render() {
    if (!this.state.inputTypes) {
      return <Spinner />;
    }

    const input = this.props.input;
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
          <Button bsStyle="info">Show received messages</Button>
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
          <Button bsStyle="info">Manage extractors</Button>
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

    const inputForm = definition ?
      (<InputForm ref="configurationForm" key={`edit-form-input-${input.id}`}
                   globalValue={input.global} nodeValue={input.node}
                   configFields={definition.requested_configuration}
                   title={`Editing Input ${input.title}`}
                   titleValue={input.title}
                   typeName={input.type} includeTitleField
                   submitAction={this._updateInput} values={input.attributes} />) : null;

    const additionalContent = (
      <div>
        <Col md={8}>
          <ConfigurationWell className="configuration-well"
                             id={input.id}
                             configuration={input.attributes}
                             typeDefinition={definition || {}} />
          <StaticFieldForm ref="staticFieldForm" input={this.props.input} />
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
