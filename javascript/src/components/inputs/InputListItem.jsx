import React, {PropTypes} from 'react';
import {Label, Button, DropdownButton, MenuItem} from 'react-bootstrap';
import {LinkContainer} from 'react-router-bootstrap';
import EntityListItem from 'components/common/EntityListItem';
import PermissionsMixin from 'util/PermissionsMixin';
import jsRoutes from 'routing/jsRoutes';
import Routes from 'routing/Routes';

const InputListItem = React.createClass({
  propTypes: {
    input: PropTypes.object.isRequired,
    currentNode: PropTypes.object.isRequired,
    permissions: PropTypes.array.isRequired,
  },
  mixins: [PermissionsMixin],
  _labelClassForState(state) {
    switch (state) {
    case 'RUNNING':
      return 'success';
    case 'FAILED':
      return 'danger';
    case 'STARTING':
      return 'info';
    default:
      return 'warning';
    }
  },

  _getMoreActionsOption(condition, component) {
    return (condition ? component : null);
  },

  render() {
    // TODO:
    // - Input state controls
    // - Input configuration
    // - Input metrics

    const inputLabel = (
      <Label bsStyle={this._labelClassForState(this.props.input.state)}
             bsSize="xsmall">{this.props.input.state.toLowerCase()}</Label>
    );

    const actions = [];

    if (this.isPermitted(this.props.permissions, ['searches:relative'])) {
      actions.push(
        <LinkContainer key={`received-messages-${this.props.input.id}`}
                       to={jsRoutes.controllers.SearchController.index(`gl2_source_input:${this.props.input.id}`, 'relative', 28800).url}>
          <Button bsStyle="info">Show received messages</Button>
        </LinkContainer>
      );
    }

    if (this.isPermitted(this.props.permissions, [`inputs:edit:${this.props.input.id}`])) {
      actions.push(
        <LinkContainer key={`manage-extractors-${this.props.input.id}`}
                       to={Routes.local_input_extractors(this.props.currentNode.node_id, this.props.input.id)}>
          <Button bsStyle="info">Manage extractors</Button>
        </LinkContainer>
      );
    }

    actions.push(
      <DropdownButton key={`more-actions-${this.props.input.id}`}
                      title="More actions"
                      id={`more-actions-dropdown-${this.props.input.id}`}
                      pullRight>
        {this.isPermitted(this.props.permissions, [`inputs:edit:${this.props.input.id}`]) && <MenuItem>Edit input</MenuItem>}
        {!this.props.input.message_input.global && <MenuItem href="">Show metrics</MenuItem>}
        {this.isPermitted(this.props.permissions, [`inputs:edit:${this.props.input.id}`]) && <MenuItem>Add static field</MenuItem>}
        {this.isPermitted(this.props.permissions, [`inputs:terminate`]) && <MenuItem>Delete input</MenuItem>}
      </DropdownButton>
    );

    const subtitle = (
      <span title={this.props.currentNode.node_id}>
        On node{' '}
        {this.props.currentNode.is_master && <i className="fa fa-star master-node" title="Master Node"></i>}
        <LinkContainer to={Routes.node(this.props.currentNode.node_id)}>
          <a>{this.props.currentNode.short_node_id}</a>
        </LinkContainer>
      </span>
    );

    return (
      <EntityListItem key={`entry-list-${this.props.input.id}`}
                      title={`${this.props.input.message_input.title} (${this.props.input.message_input.name})`}
                      titleSuffix={inputLabel}
                      description={subtitle}
                      createdFromContentPack={!!this.props.input.message_input.content_pack}
                      actions={actions}/>
    );
  },
});

export default InputListItem;
