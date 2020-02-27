// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import { ViewStore } from 'views/stores/ViewStore';
import { SearchStore } from 'views/stores/SearchStore';
import connect from 'stores/connect';

import { Modal } from 'components/graylog';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import type { ViewStoreState } from 'views/stores/ViewStore';
import type { SearchStoreState } from 'views/stores/SearchStore';

type Props = {
  currentView: ViewStoreState,
  searches: SearchStoreState,
  modalRef: BootstrapModalWrapper
}
const DebugOverlay = ({ currentView, searches, modalRef }: Props) => (
  <BootstrapModalWrapper ref={modalRef}>
    <Modal.Body>
      <textarea disabled
                style={{ height: '80vh', width: '100%' }}
                value={JSON.stringify({ currentView, searches }, null, 2)} />
    </Modal.Body>
  </BootstrapModalWrapper>
);

DebugOverlay.propTypes = {
  currentView: PropTypes.object.isRequired,
  searches: PropTypes.object.isRequired,
};

export default connect(DebugOverlay, { currentView: ViewStore, searches: SearchStore });
