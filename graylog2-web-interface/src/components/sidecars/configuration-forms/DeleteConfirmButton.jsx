import PropTypes from 'prop-types';
import React from 'react';
import { Button } from 'react-bootstrap';

class DeleteConfirmButton extends React.Component {
  static propTypes = {
    entity: PropTypes.object.isRequired,
    type: PropTypes.string.isRequired,
    onClick: PropTypes.func.isRequired,
  };

  handleClick = () => {
    if (window.confirm(`You are about to delete ${this.props.type} "${this.props.entity.name}". Are you sure?`)) {
      this.props.onClick(this.props.entity);
    }
  };

  render() {
    return (
      <Button bsStyle="primary" bsSize="xsmall" onClick={this.handleClick}>
        Delete
      </Button>
    );
  }
}

export default DeleteConfirmButton;
