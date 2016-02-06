import React, {Component, PropTypes} from 'react';
import {Modal} from 'react-bootstrap';

/**
 * Encapsulates a react-bootstrap modal, hiding the state handling for the modal
 */
class BootstrapModalWrapper extends Component {
  constructor(props) {
    super(props);

    this.open = this.open.bind(this);
    this.close = this.close.bind(this);

    this.state = {
      showModal: props.showModal || false,
    };
  }

  onOpen() {
    if (typeof this.props.onOpen === 'function') {
      this.props.onOpen();
    }
  }

  onClose() {
    if (typeof this.props.onClose === 'function') {
      this.props.onClose();
    }
  }

  open() {
    this.setState({showModal: true}, this.onOpen);
  }

  close() {
    this.setState({showModal: false}, this.onClose);
  }

  render() {
    return (
      <Modal show={this.state.showModal} onHide={this.close}>
        {this.props.children}
      </Modal>
    );
  }
}

BootstrapModalWrapper.propTypes = {
  showModal: PropTypes.bool,
  children: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.element),
    PropTypes.element,
  ]).isRequired,
  onOpen: PropTypes.func,
  onClose: PropTypes.func,
};

export default BootstrapModalWrapper;
