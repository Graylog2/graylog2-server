import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import { Modal } from 'react-bootstrap';

import { ViewStore } from 'views/stores/ViewStore';
import { SearchStore } from 'views/stores/SearchStore';
import connect from 'stores/connect';

const DebugOverlay = createReactClass({
  displayName: 'DebugOverlay',

  propTypes: {
    show: PropTypes.bool.isRequired,
    onClose: PropTypes.func,
  },

  getDefaultProps() {
    return {
      onClose: () => {},
    };
  },

  render() {
    const modalBody = this.props.show && (
      <textarea disabled
                style={{ height: '600', width: '100%' }}
                value={JSON.stringify(this.props, null, 2)} />
    );
    return (
      <Modal onHide={this.props.onClose} show={this.props.show}>
        <Modal.Body>
          {modalBody}
        </Modal.Body>
      </Modal>
    );
  },
});

export default connect(DebugOverlay, { views: ViewStore, searches: SearchStore });
