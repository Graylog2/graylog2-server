/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import PropTypes from 'prop-types';

import { Modal, Button } from 'components/bootstrap';
import type View from 'views/logic/views/View';
import SavedSearchesList from 'views/components/searchbar/saved-search/SavedSearchesList';

type Props = {
  toggleModal: () => void,
  deleteSavedSearch: (view: View) => Promise<View>,
  activeSavedSearchId: string,
};

const SavedSearchesModal = ({ toggleModal, deleteSavedSearch, activeSavedSearchId }: Props) => (
  <Modal show
         bsSize="large"
         data-app-section="saved_searches"
         data-event-element="Saved Searches List">
    <Modal.Body>
      <SavedSearchesList deleteSavedSearch={deleteSavedSearch}
                         activeSavedSearchId={activeSavedSearchId}
                         onLoadSavedSearch={toggleModal} />

    </Modal.Body>
    <Modal.Footer>
      <Button onClick={toggleModal}>Cancel</Button>
    </Modal.Footer>
  </Modal>
);

SavedSearchesModal.propTypes = {
  toggleModal: PropTypes.func.isRequired,
  deleteSavedSearch: PropTypes.func.isRequired,
};

export default SavedSearchesModal;
