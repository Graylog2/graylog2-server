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
import { useTheme } from 'styled-components';
import { useState, useContext, useRef } from 'react';

import { useStore } from 'stores/connect';
import { isPermitted } from 'util/PermissionsMixin';
import { Button, ButtonGroup, DropdownButton, MenuItem } from 'components/bootstrap';
import { Icon, ShareButton } from 'components/common';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import UserNotification from 'util/UserNotification';
import { ViewStore, ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import onSaveView from 'views/logic/views/OnSaveViewAction';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import ExportModal from 'views/components/export/ExportModal';
import ViewTypeLabel from 'views/components/ViewTypeLabel';
import EntityShareModal from 'components/permissions/EntityShareModal';
import CurrentUserContext from 'contexts/CurrentUserContext';
import * as ViewsPermissions from 'views/Permissions';
import type User from 'logic/users/User';
import ViewPropertiesModal from 'views/components/views/ViewPropertiesModal';
import { loadAsDashboard, loadNewSearch } from 'views/logic/views/Actions';
import IfPermitted from 'components/common/IfPermitted';

import SavedSearchForm from './SavedSearchForm';
import SavedSearchList from './SavedSearchList';

const _isAllowedToEdit = (view: View, currentUser: User | undefined | null) => (
  view.owner === currentUser?.username
  || isPermitted(currentUser?.permissions, [ViewsPermissions.View.Edit(view.id)])
);

const SavedSearchControls = () => {
  const theme = useTheme();
  const { view, dirty } = useStore(ViewStore);
  const viewLoaderFunc = useContext(ViewLoaderContext);
  const currentUser = useContext(CurrentUserContext);
  const loadNewView = useContext(NewViewLoaderContext);
  const isAllowedToEdit = (view && view.id) && _isAllowedToEdit(view, currentUser);
  const formTarget = useRef();
  const [showForm, setShowForm] = useState(false);
  const [showList, setShowList] = useState(false);
  const [showExport, setShowExport] = useState(false);
  const [showMetadataEdit, setShowMetadataEdit] = useState(false);
  const [showShareSearch, setShowShareSearch] = useState(false);
  const [newTitle, setNewTitle] = useState((view && view.title) || '');

  const loaded = (view && view.id);
  const viewTypeLabel = ViewTypeLabel({ type: view?.type });
  const savedSearchColor = dirty ? theme.colors.variant.warning : theme.colors.variant.info;
  const disableReset = !(dirty || loaded);
  const savedViewTitle = loaded ? 'Saved search' : 'Save search';
  const title = dirty ? 'Unsaved changes' : savedViewTitle;

  const toggleFormModal = () => setShowForm((cur) => !cur);
  const toggleListModal = () => setShowList((cur) => !cur);
  const toggleExport = () => setShowExport((cur) => !cur);
  const toggleMetadataEdit = () => setShowMetadataEdit((cur) => !cur);
  const toggleShareSearch = () => setShowShareSearch((cur) => !cur);
  const onChangeTitle = (e: React.ChangeEvent<HTMLInputElement>) => setNewTitle(e.target.value);

  const _extractErrorMessage = (error) => {
    return (error
      && error.additional
      && error.additional.body
      && error.additional.body.message) ? error.additional.body.message : error;
  };

  const saveSearch = () => {
    if (!view.id) {
      return;
    }

    const newView = view.toBuilder()
      .title(newTitle)
      .type(View.Type.Search)
      .build();

    ViewManagementActions.update(newView)
      .then(toggleFormModal)
      .then(() => UserNotification.success(`Saving view "${newView.title}" was successful!`, 'Success!'))
      .catch((error) => UserNotification.error(`Saving view failed: ${_extractErrorMessage(error)}`, 'Error!'));
  };

  const saveAsSearch = () => {
    if (!newTitle || newTitle === '') {
      return;
    }

    const newView = view.toBuilder()
      .newId()
      .title(newTitle)
      .type(View.Type.Search)
      .build();

    ViewManagementActions.create(newView)
      .then((createdView) => {
        toggleFormModal();

        return createdView;
      })
      .then((createdView) => {
        viewLoaderFunc(createdView.id);
      })
      .then(() => UserNotification.success(`Saving view "${newView.title}" was successful!`, 'Success!'))
      .catch((error) => UserNotification.error(`Saving view failed: ${_extractErrorMessage(error)}`, 'Error!'));
  };

  const deleteSavedSearch = (deletedView: View) => {
    return ViewManagementActions.delete(deletedView)
      .then(() => UserNotification.success(`Deleting view "${deletedView.title}" was successful!`, 'Success!'))
      .then(() => ViewActions.create(View.Type.Search))
      .then(() => {
        if (deletedView.id === view.id) {
          loadNewSearch();
        }
      })
      .catch((error) => UserNotification.error(`Deleting view failed: ${_extractErrorMessage(error)}`, 'Error!'));
  };

  const _loadAsDashboard = () => {
    loadAsDashboard(view);
  };

  return (
    <ButtonGroup aria-label="Search Meta Buttons">
      <Button title={title} ref={formTarget} onClick={toggleFormModal}>
        <Icon style={{ color: loaded ? savedSearchColor : undefined }} name="star" type={loaded ? 'solid' : 'regular'} /> Save
      </Button>
      {showForm && (
        <SavedSearchForm onChangeTitle={onChangeTitle}
                         target={formTarget.current}
                         saveSearch={saveSearch}
                         saveAsSearch={saveAsSearch}
                         disableCreateNew={newTitle === view.title}
                         isCreateNew={!view.id || !isAllowedToEdit}
                         toggleModal={toggleFormModal}
                         value={newTitle} />
      )}
      <Button title="Load a previously saved search"
              onClick={toggleListModal}>
        <Icon name="folder" type="regular" /> Load
      </Button>
      {showList && (
        <SavedSearchList deleteSavedSearch={deleteSavedSearch}
                         toggleModal={toggleListModal}
                         activeSavedSearchId={view.id} />
      )}
      <ShareButton entityType="search"
                   entityId={view.id}
                   onClick={toggleShareSearch}
                   bsStyle="default"
                   disabledInfo={!view.id && 'Only saved searches can be shared.'} />
      <DropdownButton title={<Icon name="ellipsis-h" />} aria-label="Open search actions dropdown" id="search-actions-dropdown" pullRight noCaret>
        <MenuItem onSelect={toggleMetadataEdit} disabled={!isAllowedToEdit}>
          <Icon name="edit" /> Edit metadata
        </MenuItem>
        <IfPermitted permissions="dashboards:create">
          <MenuItem onSelect={_loadAsDashboard}><Icon name="tachometer-alt" /> Export to dashboard</MenuItem>
        </IfPermitted>
        <MenuItem onSelect={toggleExport}><Icon name="cloud-download-alt" /> Export</MenuItem>
        <MenuItem disabled={disableReset} onSelect={() => loadNewView()}>
          <Icon name="eraser" /> Reset search
        </MenuItem>
        <MenuItem divider />
      </DropdownButton>
      {showExport && (<ExportModal view={view} closeModal={toggleExport} />)}
      {showMetadataEdit && (
        <ViewPropertiesModal show
                             view={view}
                             title="Editing saved search"
                             onClose={toggleMetadataEdit}
                             onSave={onSaveView} />
      )}
      {showShareSearch && (
        <EntityShareModal entityId={view.id}
                          entityType="search"
                          entityTitle={view.title}
                          description={`Search for a User or Team to add as collaborator on this ${viewTypeLabel}.`}
                          onClose={toggleShareSearch} />
      )}
    </ButtonGroup>
  );
};

export default SavedSearchControls;
