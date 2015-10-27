import React, {PropTypes} from 'react';
import {Label, Button, DropdownButton, MenuItem, Col, Well} from 'react-bootstrap';
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

  _getMoreActionsItems() {
    const items = [];

    if (this.isPermitted(this.props.permissions, [`inputs:edit:${this.props.input.id}`])) {
      items.push(<MenuItem key={`edit-input-${this.props.input.id}`}>Edit input</MenuItem>);
    }
    if (!this.props.input.message_input.global) {
      items.push(<MenuItem key={`show-metrics-${this.props.input.id}`} href="">Show metrics</MenuItem>);
    }
    if (this.isPermitted(this.props.permissions, [`inputs:edit:${this.props.input.id}`])) {
      items.push(<MenuItem key={`add-static-field-${this.props.input.id}`}>Add static field</MenuItem>);
    }
    if (this.isPermitted(this.props.permissions, [`inputs:terminate`])) {
      items.push(<MenuItem key={`divider-${this.props.input.id}`} divider/>);
      items.push(<MenuItem key={`delete-input-${this.props.input.id}`}>Delete input</MenuItem>);
    }

    return items;
  },

  _getConfigurationOptions(inputAttributes) {
    const attributes = Object.keys(inputAttributes);
    return attributes.map(attribute => {
      return <li key={`${attribute}-${this.props.input.id}`}>{attribute}: {inputAttributes[attribute]}</li>;
    });
  },

  render() {
    // TODO:
    // - Input state controls
    // - Input metrics
    // - Show number of nodes where global input is running

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
                      className="more-actions"
                      pullRight>
        {this._getMoreActionsItems()}
      </DropdownButton>
    );

    let subtitle;

    if (!this.props.input.message_input.global) {
      subtitle = (
        <span title={this.props.currentNode.node_id}>
          On node{' '}
          {this.props.currentNode.is_master && <i className="fa fa-star master-node" title="Master Node"></i>}
          <LinkContainer to={Routes.node(this.props.currentNode.node_id)}>
            <a>{this.props.currentNode.short_node_id}</a>
          </LinkContainer>
        </span>
      );
    }

    const additionalContent = (
      <div>
        <Col md={8}>
          <Well bsSize="small" className="configuration-well">
            <ul>
              {this._getConfigurationOptions(this.props.input.message_input.attributes)}
            </ul>
          </Well>
        </Col>
        <Col md={4}>
          <div className="graylog-input-metrics">
            <h3>Throughput / Metrics</h3>
            <div className="react-input-metrics" data-input-id="@input.getId" data-input-classname="@input.getType"
                 data-node-id="@inputState.getNode.getNodeId"></div>
          </div>
        </Col>
      </div>
    );

    return (
      <EntityListItem key={`entry-list-${this.props.input.id}`}
                      title={`${this.props.input.message_input.title} (${this.props.input.message_input.name})`}
                      titleSuffix={inputLabel}
                      description={subtitle}
                      createdFromContentPack={!!this.props.input.message_input.content_pack}
                      actions={actions}
                      contentRow={additionalContent}/>
    );
  },
});

export default InputListItem;
