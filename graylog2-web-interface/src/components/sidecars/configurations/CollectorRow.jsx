import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { LinkContainer } from 'components/graylog/router';
import { ButtonToolbar, DropdownButton, MenuItem, Button } from 'components/graylog';
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
    const { onClone, collector } = this.props;

    onClone(collector);
  },

  handleDelete() {
    const { onDelete, collector } = this.props;

    // eslint-disable-next-line no-alert
    if (window.confirm(`You are about to delete collector "${collector.name}". Are you sure?`)) {
      onDelete(collector);
    }
  },

  render() {
    const { collector, validateCollector, onClone } = this.props;

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
              <CopyCollectorModal collector={collector}
                                  validateCollector={validateCollector}
                                  copyCollector={onClone} />
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
