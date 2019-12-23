import PropTypes from 'prop-types';
import React from 'react';

import { Modal } from 'components/graylog';

/**
 * Encapsulates a react-bootstrap modal, hiding the state handling for the modal
 */
class BootstrapModalWrapper extends React.Component {
  static propTypes = {
    showModal: PropTypes.bool,
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
    onOpen: PropTypes.func,
    onClose: PropTypes.func,
    onHide: PropTypes.func,
    bsSize: PropTypes.oneOf([
      'large', 'lg', 'small', 'sm',
    ]),
    backdrop: PropTypes.oneOf(['static', true, false]),
  };

  static defaultProps = {
    showModal: false,
    onOpen: () => {},
    onClose: () => {},
    onHide: () => {},
    bsSize: undefined,
    backdrop: 'static',
  };

  state = {
    // eslint-disable-next-line react/destructuring-assignment
    showModal: this.props.showModal || false,
  };

  open = () => {
    const { onOpen } = this.props;
    this.setState({ showModal: true }, onOpen);
  };

  close = () => {
    const { onClose } = this.props;
    this.setState({ showModal: false }, onClose);
  };

  hide = () => {
    const { onHide } = this.props;
    this.setState({ showModal: false }, onHide);
  };

  render() {
    const { showModal } = this.state;
    const { children, bsSize, backdrop } = this.props;
    return (
      <Modal show={showModal}
             onHide={this.hide}
             bsSize={bsSize}
             backdrop={backdrop}>
        {children}
      </Modal>
    );
  }
}

export default BootstrapModalWrapper;
