import PropTypes from 'prop-types';
import React from 'react';
import { Modal } from 'react-bootstrap';

/**
 * Encapsulates a react-bootstrap modal, hiding the state handling for the modal
 */
class BootstrapModalWrapper extends React.Component {
  static defaultProps = {
    showModal: false,
    onOpen: () => {},
    onClose: () => {},
    onHide: () => {},
  }

  static propTypes = {
    showModal: PropTypes.bool,
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
    onOpen: PropTypes.func,
    onClose: PropTypes.func,
    onHide: PropTypes.func,
  }

  state = {
    showModal: this.props.showModal || false,
  }

  open = () => {
    this.setState({ showModal: true }, this.props.onOpen);
  }

  close = () => {
    this.setState({ showModal: false }, this.props.onClose);
  }

  hide = () => {
    this.setState({ showModal: false }, this.props.onHide);
  }

  render() {
    return (
      <Modal show={this.state.showModal} onHide={this.hide}>
        {this.props.children}
      </Modal>
    );
  }
}

export default BootstrapModalWrapper;
