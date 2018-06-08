import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Button, ButtonToolbar, DropdownButton, MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';

import OperatingSystemIcon from 'components/sidecars/common/OperatingSystemIcon';
import CopyCollectorModal from './CopyCollectorModal';

const CollectorRow = createReactClass({
  propTypes: {
    collector: PropTypes.object.isRequired,
    onClone: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
    validateCollector: PropTypes.func.isRequired,
  },

  handleClone() {
    this.props.onClone(this.props.collector);
  },

  handleDelete() {
    const collector = this.props.collector;
    if (window.confirm(`You are about to delete collector "${collector.name}". Are you sure?`)) {
      this.props.onDelete(collector);
    }
  },

  render() {
    const { collector } = this.props;
    return (
      <tr>
        <td>
          {collector.name}
        </td>
        <td>
          <OperatingSystemIcon operatingSystem={collector.node_operating_system} /> {lodash.upperFirst(collector.node_operating_system)}
        </td>
        <td>
          <ButtonToolbar>
            <LinkContainer to={Routes.SYSTEM.SIDECARS.EDIT_COLLECTOR(collector.id)}>
              <Button bsStyle="info" bsSize="xsmall">Edit</Button>
            </LinkContainer>
            <DropdownButton id={`more-actions-${collector.id}`} title="More actions" bsSize="xsmall" pullRight>
              <CopyCollectorModal id={collector.id}
                                  validateCollector={this.props.validateCollector}
                                  copyCollector={this.props.onClone} />
              <MenuItem divider />
              <MenuItem onSelect={this.handleDelete}>Delete</MenuItem>
            </DropdownButton>
          </ButtonToolbar>
        </td>
      </tr>
    );
  },
});

export default CollectorRow;
