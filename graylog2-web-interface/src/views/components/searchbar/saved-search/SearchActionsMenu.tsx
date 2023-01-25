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
import styled, { useTheme } from 'styled-components';
import { useCallback, useState, useContext, useRef } from 'react';

import { isPermitted } from 'util/PermissionsMixin';
import { Button, ButtonGroup, DropdownButton, MenuItem } from 'components/bootstrap';
import { Icon, ShareButton } from 'components/common';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import UserNotification from 'util/UserNotification';
import View from 'views/logic/views/View';
import onSaveView from 'views/logic/views/OnSaveViewAction';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import ExportModal from 'views/components/export/ExportModal';
import EntityShareModal from 'components/permissions/EntityShareModal';
import useCurrentUser from 'hooks/useCurrentUser';
import * as ViewsPermissions from 'views/Permissions';
import type User from 'logic/users/User';
import ViewPropertiesModal from 'views/components/dashboard/DashboardPropertiesModal';
import { loadAsDashboard, loadNewSearch } from 'views/logic/views/Actions';
import IfPermitted from 'components/common/IfPermitted';
import {
  executePluggableSearchDuplicationHandler as executePluggableDuplicationHandler,
} from 'views/logic/views/pluggableSaveViewFormHandler';
import useSaveViewFormControls from 'views/hooks/useSaveViewFormControls';
import useIsDirty from 'views/hooks/useIsDirty';
import useIsNew from 'views/hooks/useIsNew';
import useView from 'views/hooks/useView';
import useAppDispatch from 'stores/useAppDispatch';

import SavedSearchForm from './SavedSearchForm';
import SavedSearchesModal from './SavedSearchesModal';

const Container = styled(ButtonGroup)`
  display: flex;
  justify-content: flex-end;
`;

const _isAllowedToEdit = (view: View, currentUser: User | undefined | null) => (
  view.owner === currentUser?.username
  || isPermitted(currentUser?.permissions, [ViewsPermissions.View.Edit(view.id)])
);

const SearchActionsMenu = () => {
  const theme = useTheme();
  const dirty = useIsDirty();
  const view = useView();
  const isNew = useIsNew();
  const viewLoaderFunc = useContext(ViewLoaderContext);
  const currentUser = useCurrentUser();
  const loadNewView = useContext(NewViewLoaderContext);
  const isAllowedToEdit = (view && view.id) && _isAllowedToEdit(view, currentUser);
  const formTarget = useRef();
  const [showForm, setShowForm] = useState(false);
  const [showList, setShowList] = useState(false);
  const [showExport, setShowExport] = useState(false);
  const [showMetadataEdit, setShowMetadataEdit] = useState(false);
  const [showShareSearch, setShowShareSearch] = useState(false);
  const [newTitle, setNewTitle] = useState((view && view.title) || '');
  const dispatch = useAppDispatch();
  const _onSaveView = useCallback(() => dispatch(onSaveView(view)), [dispatch, view]);

  const loaded = isNew === false;
  const savedSearchColor = dirty ? theme.colors.variant.dark.warning : theme.colors.variant.info;
  const disableReset = !(dirty || loaded);
  const savedViewTitle = loaded ? 'Saved search' : 'Save search';
  const title = dirty ? 'Unsaved changes' : savedViewTitle;
  const pluggableSaveViewControls = useSaveViewFormControls();

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

  const saveAsSearch = async () => {
    if (!newTitle || newTitle === '') {
      return;
    }

    const viewWithPluginData = await executePluggableDuplicationHandler(view, currentUser.permissions, pluggableSaveViewControls);

    const newView = viewWithPluginData.toBuilder()
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
      .then(() => {
        if (deletedView.id === view.id) {
          loadNewSearch();
        }

        return Promise.resolve();
      })
      .catch((error) => UserNotification.error(`Deleting view failed: ${_extractErrorMessage(error)}`, 'Error!'));
  };

  const _loadAsDashboard = () => {
    loadAsDashboard(view);
  };

  return (
    <Container aria-label="Search Meta Buttons">
      <Button title={title} ref={formTarget} onClick={toggleFormModal}>
        <Icon style={{ color: loaded ? savedSearchColor : undefined }} name="floppy-disk" type={loaded ? 'solid' : 'regular'} /> Save
      </Button>
      {showForm && (
        <SavedSearchForm onChangeTitle={onChangeTitle}
                         target={formTarget.current}
                         saveSearch={saveSearch}
                         saveAsSearch={saveAsSearch}
                         disableCreateNew={newTitle === view.title}
                         isCreateNew={isNew || !isAllowedToEdit}
                         toggleModal={toggleFormModal}
                         value={newTitle} />
      )}
      <Button title="Load a previously saved search"
              onClick={toggleListModal}>
        <Icon name="folder" type="regular" /> Load
      </Button>
      {showList && (
        <SavedSearchesModal deleteSavedSearch={deleteSavedSearch}
                            toggleModal={toggleListModal}
                            activeSavedSearchId={view.id} />
      )}
      <ShareButton entityType="search"
                   entityId={view.id}
                   onClick={toggleShareSearch}
                   bsStyle="default"
                   disabledInfo={isNew && 'Only saved searches can be shared.'} />
      <DropdownButton title={<Icon name="ellipsis-h" />}
                      aria-label="Open search actions dropdown"
                      id="search-actions-dropdown"
                      pullRight
                      noCaret>
        <MenuItem onSelect={toggleMetadataEdit} disabled={!isAllowedToEdit} icon="edit">
          Edit metadata
        </MenuItem>
        <IfPermitted permissions="dashboards:create">
          <MenuItem onSelect={_loadAsDashboard} icon="tachometer-alt">Export to dashboard</MenuItem>
        </IfPermitted>
        <MenuItem onSelect={toggleExport} icon="cloud-download-alt">Export</MenuItem>
        <MenuItem disabled={disableReset} onSelect={loadNewView} icon="eraser">
          Reset search
        </MenuItem>
        <MenuItem divider />
      </DropdownButton>
      {showExport && (<ExportModal view={view} closeModal={toggleExport} />)}
      {showMetadataEdit && (
        <ViewPropertiesModal show
                             view={view}
                             title="Editing saved search"
                             submitButtonText="Update search"
                             onClose={toggleMetadataEdit}
                             onSave={_onSaveView} />
      )}
      {showShareSearch && (
        <EntityShareModal entityId={view.id}
                          entityType="search"
                          entityTitle={view.title}
                          description="Search for a User or Team to add as collaborator on this saved search."
                          onClose={toggleShareSearch} />
      )}
    </Container>
  );
};

export default SearchActionsMenu;
