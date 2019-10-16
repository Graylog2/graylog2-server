import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import { Modal } from 'components/graylog';

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
    const { show, onClose } = this.props;
    const modalBody = show && (
      <textarea disabled
                style={{ height: '80vh', width: '100%' }}
                value={JSON.stringify(this.props, null, 2)} />
    );
    return (
      <Modal onHide={onClose} show={show}>
        <Modal.Body>
          {modalBody}
        </Modal.Body>
      </Modal>
    );
  },
});

export default connect(DebugOverlay, { views: ViewStore, searches: SearchStore });
