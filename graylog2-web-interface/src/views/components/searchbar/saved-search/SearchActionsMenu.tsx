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
import { useCallback, useState, useContext, useRef, useMemo } from 'react';

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
import { loadView, updateView } from 'views/logic/slices/viewSlice';
import type FetchError from 'logic/errors/FetchError';
import useHistory from 'routing/useHistory';
import usePluginEntities from 'hooks/usePluginEntities';

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

const _extractErrorMessage = (error: FetchError) => {
  return (error
    && error.additional
    && error.additional.body
    && error.additional.body.message) ? error.additional.body.message : error;
};

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
  const currentTitle = view?.title ?? '';
  const dispatch = useAppDispatch();
  const onUpdateView = useCallback((newView: View) => dispatch(updateView(newView)), [dispatch]);

  const loaded = isNew === false;
  const savedSearchColor = dirty ? theme.colors.variant.dark.warning : theme.colors.variant.info;
  const disableReset = !(dirty || loaded);
  const savedViewTitle = loaded ? 'Saved search' : 'Save search';
  const title = dirty ? 'Unsaved changes' : savedViewTitle;
  const pluggableSaveViewControls = useSaveViewFormControls();
  const history = useHistory();

  const toggleFormModal = useCallback(() => setShowForm((cur) => !cur), []);
  const toggleListModal = useCallback(() => setShowList((cur) => !cur), []);
  const toggleExport = useCallback(() => setShowExport((cur) => !cur), []);
  const toggleMetadataEdit = useCallback(() => setShowMetadataEdit((cur) => !cur), []);
  const toggleShareSearch = useCallback(() => setShowShareSearch((cur) => !cur), []);

  const pluggableSearchActions = usePluginEntities('views.components.searchActions');
  const searchActions = useMemo(() => pluggableSearchActions.map((PluggableSearchAction) => <PluggableSearchAction loaded={loaded} view={view} />), [pluggableSearchActions, loaded, view]);

  const saveSearch = useCallback(async (newTitle: string) => {
    if (!view.id) {
      return;
    }

    const newView = view.toBuilder()
      .title(newTitle)
      .type(View.Type.Search)
      .build();

    await dispatch(onSaveView(newView));
    toggleFormModal();
    await dispatch(loadView(newView));
  }, [dispatch, toggleFormModal, view]);

  const saveAsSearch = useCallback(async (newTitle: string) => {
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
  }, [currentUser.permissions, pluggableSaveViewControls, toggleFormModal, view, viewLoaderFunc]);

  const deleteSavedSearch = useCallback((deletedView: View) => {
    return ViewManagementActions.delete(deletedView)
      .then(() => UserNotification.success(`Deleting view "${deletedView.title}" was successful!`, 'Success!'))
      .then(() => {
        if (deletedView.id === view.id) {
          loadNewSearch(history);
        }

        return Promise.resolve();
      })
      .catch((error) => UserNotification.error(`Deleting view failed: ${_extractErrorMessage(error)}`, 'Error!'));
  }, [history, view.id]);

  const _loadAsDashboard = useCallback(() => {
    loadAsDashboard(history, view);
  }, [history, view]);

  return (
    <Container aria-label="Search Meta Buttons">
      <Button title={title} ref={formTarget} onClick={toggleFormModal}>
        <Icon style={{ color: loaded ? savedSearchColor : undefined }} name="floppy-disk" type={loaded ? 'solid' : 'regular'} /> Save
      </Button>
      {showForm && (
        <SavedSearchForm target={formTarget.current}
                         saveSearch={saveSearch}
                         saveAsSearch={saveAsSearch}
                         isCreateNew={isNew || !isAllowedToEdit}
                         toggleModal={toggleFormModal}
                         value={currentTitle} />
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
        {searchActions.length > 0 ? (
          <>
            <MenuItem divider />
            {searchActions}
          </>
        ) : null}
      </DropdownButton>
      {showExport && (<ExportModal view={view} closeModal={toggleExport} />)}
      {showMetadataEdit && (
        <ViewPropertiesModal show
                             view={view}
                             title="Editing saved search"
                             submitButtonText="Update search"
                             onClose={toggleMetadataEdit}
                             onSave={onUpdateView} />
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
