import React from 'react';
import Reflux from 'reflux';
import { Button, Modal } from 'react-bootstrap';

import CurrentViewStore from 'enterprise/stores/CurrentViewStore';
import QueriesStore from 'enterprise/stores/QueriesStore';
import SearchStore from 'enterprise/stores/SearchStore';
import WidgetStore from 'enterprise/stores/WidgetStore';
import ViewsStore from 'enterprise/stores/ViewsStore';

const DebugOverlay = React.createClass({
  mixins: [
    Reflux.connect(CurrentViewStore, 'currentView'),
    Reflux.connect(QueriesStore, 'queries'),
    Reflux.connect(SearchStore, 'search'),
    Reflux.connect(ViewsStore, 'views'),
    Reflux.connect(WidgetStore, 'widgets'),
  ],
  _onOpen() {
    this.setState({ open: true });
  },
  _onClose() {
    this.setState({ open: false });
  },
  render() {
    return (
      <span>
        <Button onClick={this._onOpen}>Debug</Button>
        <Modal onHide={this._onClose} show={this.state.open}>
          <Modal.Body>
            <textarea disabled style={{ height: '600', width: '100%' }}>
              {JSON.stringify(this.state, null, 2)}
            </textarea>
          </Modal.Body>
        </Modal>
      </span>
    );
  },
});

export default DebugOverlay;
