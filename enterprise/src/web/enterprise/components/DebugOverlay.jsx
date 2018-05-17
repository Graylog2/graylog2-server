import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Modal } from 'react-bootstrap';

import { ViewStore } from 'enterprise/stores/ViewStore';
import { SearchStore } from 'enterprise/stores/SearchStore';

const DebugOverlay = createReactClass({
  displayName: 'DebugOverlay',

  propTypes: {
    show: PropTypes.bool.isRequired,
    onClose: PropTypes.func,
  },

  mixins: [
    Reflux.connect(ViewStore, 'views'),
    Reflux.connect(SearchStore, 'searches'),
  ],

  getDefaultProps() {
    return {
      onClose: () => {},
    };
  },

  render() {
    const state = {
      view: this.state.views,
      searches: {
        search: this.state.searches.search,
        widgetMapping: this.state.searches.widgetMapping,
      },
    };
    const modalBody = this.props.show && (
      <textarea disabled style={{ height: '600', width: '100%' }}>
        {JSON.stringify(state, null, 2)}
      </textarea>
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

export default DebugOverlay;
