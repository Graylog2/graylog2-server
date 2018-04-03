import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Button, Modal } from 'react-bootstrap';

import CurrentViewStore from 'enterprise/stores/CurrentViewStore';
import QueriesStore from 'enterprise/stores/QueriesStore';
import SearchStore from 'enterprise/stores/SearchStore';
import WidgetStore from 'enterprise/stores/WidgetStore';
import ViewsStore from 'enterprise/stores/ViewsStore';

const DebugOverlay = createReactClass({
  displayName: 'DebugOverlay',

  propTypes: {
    show: PropTypes.bool.isRequired,
    onClose: PropTypes.func,
  },

  mixins: [
    Reflux.connect(CurrentViewStore, 'currentView'),
    Reflux.connect(QueriesStore, 'queries'),
    Reflux.connect(SearchStore, 'search'),
    Reflux.connect(ViewsStore, 'views'),
    Reflux.connect(WidgetStore, 'widgets'),
  ],

  getDefaultProps() {
    return {
      onClose: () => {},
    };
  },

  render() {
    return (
      <Modal onHide={this.props.onClose} show={this.props.show}>
        <Modal.Body>
          <textarea disabled style={{ height: '600', width: '100%' }}>
            {JSON.stringify(this.state, null, 2)}
          </textarea>
        </Modal.Body>
      </Modal>
    );
  },
});

export default DebugOverlay;
