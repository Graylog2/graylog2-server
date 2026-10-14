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
import styled from 'styled-components';
import { useCallback, useState, useContext, useRef } from 'react';
import { useTranslation } from 'react-i18next';

import { Button, ButtonGroup, MenuItem } from 'components/bootstrap';
import { Icon, ShareButton } from 'components/common';
import UserNotification from 'util/UserNotification';
import View from 'views/logic/views/View';
import onSaveView from 'views/logic/views/OnSaveViewAction';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import ExportModal from 'views/components/export/ExportModal';
import EntityShareModal from 'components/permissions/EntityShareModal';
import useCurrentUser from 'hooks/useCurrentUser';
import usePermissions from 'hooks/usePermissions';
import type { IsPermitted } from 'hooks/usePermissions';
import useGetPermissionsByScope from 'hooks/useScopePermissions';
import * as ViewsPermissions from 'views/Permissions';
import type User from 'logic/users/User';
import ViewPropertiesModal from 'views/components/dashboard/DashboardPropertiesModal';
import { loadAsDashboard, loadNewSearch } from 'views/logic/views/Actions';
import IfPermitted from 'components/common/IfPermitted';
import { executePluggableSearchDuplicationHandler as executePluggableDuplicationHandler } from 'views/logic/views/pluggableSaveViewFormHandler';
import useSaveViewFormControls from 'views/hooks/useSaveViewFormControls';
import useIsDirty from 'views/hooks/useIsDirty';
import useIsNew from 'views/hooks/useIsNew';
import useView from 'views/hooks/useView';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import { loadView, updateView } from 'views/logic/slices/viewSlice';
import type FetchError from 'logic/errors/FetchError';
import useHistory from 'routing/useHistory';
import usePluginEntities from 'hooks/usePluginEntities';
import SavedSearchesModal from 'views/components/searchbar/saved-search/SavedSearchesModal';
import SaveViewButton from 'views/components/searchbar/SaveViewButton';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';
import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import useHotkey from 'hooks/useHotkey';
import { createGRN } from 'logic/permissions/GRN';
import useSelectedStreamsGRN from 'views/hooks/useSelectedStreamsGRN';
import { createView, deleteView } from 'views/api/views';
import { MoreActionsMenu } from 'components/common/MoreActions';

import SavedSearchForm from './SavedSearchForm';

const Container = styled(ButtonGroup)`
  display: flex;
  justify-content: flex-end;
`;

const _isAllowedToEdit = (view: View, currentUser: User | undefined | null, isPermitted: IsPermitted) =>
  view.owner === currentUser?.username || isPermitted([ViewsPermissions.View.Edit(view.id)]);

const _extractErrorMessage = (error: FetchError) =>
  error && error.additional && error.additional.body && error.additional.body.message
    ? error.additional.body.message
    : error;

const usePluggableSearchAction = (loaded: boolean, view: View) => {
  const modalRefs = useRef({});
  const pluggableSearchActions = usePluginEntities('views.components.searchActions');

  const actions = pluggableSearchActions
    .filter((action) => (action.useCondition ? !!action.useCondition() : true))
    .map(({ component: PluggableSearchAction, key, modals }) => {
      if (modals) {
        const refs = Object.fromEntries(
          modals.map(({ key: modalKey }) => modalKey).map((mKey) => [mKey, () => modalRefs.current[mKey]]),
        );

        return <PluggableSearchAction key={key} loaded={loaded} search={view} modalRefs={refs} />;
      }

      return <PluggableSearchAction key={key} loaded={loaded} search={view} />;
    });

  const actionModals = pluggableSearchActions
    .filter(({ modals }) => !!modals)
    .flatMap(({ modals }) => modals)
    .map(({ key, component: ActionModal }) => (
      <ActionModal
        key={key}
        search={view}
        ref={(r) => {
          modalRefs.current[key] = r;
        }}
      />
    ));

  return { actions, actionModals };
};

const SearchActionsMenu = () => {
  const dirty = useIsDirty();
  const view = useView();
  const isNew = useIsNew();
  const viewLoaderFunc = useContext(ViewLoaderContext);
  const currentUser = useCurrentUser();
  const { isPermitted } = usePermissions();
  const loadNewView = useContext(NewViewLoaderContext);
  const { scopePermissions } = useGetPermissionsByScope({ _scope: view?.scope });
  const isAllowedToEdit =
    view && view.id && _isAllowedToEdit(view, currentUser, isPermitted) && !!scopePermissions?.is_mutable;
  const formTarget = useRef();
  const [showForm, setShowForm] = useState(false);
  const [showList, setShowList] = useState(false);
  const [showExport, setShowExport] = useState(false);
  const [showMetadataEdit, setShowMetadataEdit] = useState(false);
  const [showShareSearch, setShowShareSearch] = useState(false);
  const currentTitle = view?.title ?? '';
  const dispatch = useViewsDispatch();
  const onUpdateView = useCallback((newView: View) => dispatch(updateView(newView)), [dispatch]);
  const { selectedStreamsGRN } = useSelectedStreamsGRN();
  const { t } = useTranslation('search');

  const loaded = isNew === false;
  const disableReset = !(dirty || loaded);
  const savedViewTitle = loaded ? t('actions_menu.saved_search') : t('actions_menu.save_search');
  const title = dirty ? t('actions_menu.unsaved_changes') : savedViewTitle;
  const pluggableSaveViewControls = useSaveViewFormControls();
  const history = useHistory();

  const toggleFormModal = useCallback(() => setShowForm((cur) => !cur), []);
  const closeFormModal = useCallback(() => setShowForm(false), []);
  const openFormModal = useCallback(() => setShowForm(true), []);
  const toggleListModal = useCallback(() => setShowList((cur) => !cur), []);
  const toggleExport = useCallback(() => setShowExport((cur) => !cur), []);
  const toggleMetadataEdit = useCallback(() => setShowMetadataEdit((cur) => !cur), []);
  const toggleShareSearch = useCallback(() => setShowShareSearch((cur) => !cur), []);
  const { actions: pluggableActions, actionModals: pluggableActionModals } = usePluggableSearchAction(loaded, view);

  const saveSearch = useCallback(
    async (newTitle: string, entityShare?: EntitySharePayload) => {
      if (!view.id) {
        return;
      }
      const newView = view.toBuilder().title(newTitle).type(View.Type.Search).build();

      await dispatch(onSaveView(newView, entityShare));

      if (entityShare) {
        await EntityShareDomain.update('search', title, createGRN('search', view.id), entityShare);
      }

      closeFormModal();
      await dispatch(loadView(newView));
    },
    [closeFormModal, dispatch, view, title],
  );

  const saveAsSearch = useCallback(
    async (newTitle: string, entityShare?: EntitySharePayload) => {
      if (!newTitle || newTitle === '') {
        return;
      }
      const viewWithPluginData = await executePluggableDuplicationHandler(view, isPermitted, pluggableSaveViewControls);

      const newView = viewWithPluginData.toBuilder().newId().title(newTitle).type(View.Type.Search).build();

      createView(newView, entityShare, view.id)
        .then((createdView) => {
          toggleFormModal();

          return createdView;
        })
        .then((createdView) => {
          viewLoaderFunc(createdView.id);
        })
        .then(() =>
          UserNotification.success(
            t('actions_menu.save_success', { title: newView.title }),
            t('save_view_notifications.success_title'),
          ),
        )
        .catch((error) =>
          UserNotification.error(
            t('actions_menu.save_error', { error: _extractErrorMessage(error) }),
            t('save_view_notifications.error_title'),
          ),
        );
    },
    [isPermitted, pluggableSaveViewControls, t, toggleFormModal, view, viewLoaderFunc],
  );

  const deleteSavedSearch = useCallback(
    (deletedView: View) =>
      deleteView(deletedView)
        .then(() =>
          UserNotification.success(
            t('actions_menu.delete_success', { title: deletedView.title }),
            t('save_view_notifications.success_title'),
          ),
        )
        .then(() => {
          if (deletedView.id === view.id) {
            loadNewSearch(history);
          }

          return Promise.resolve();
        })
        .catch((error) =>
          UserNotification.error(
            t('actions_menu.delete_error', { error: _extractErrorMessage(error) }),
            t('save_view_notifications.error_title'),
          ),
        ),
    [history, t, view.id],
  );

  const _loadAsDashboard = useCallback(() => {
    loadAsDashboard(history, view);
  }, [history, view]);

  useHotkey({
    actionKey: 'save',
    callback: () => (loaded && isAllowedToEdit ? saveSearch(currentTitle) : openFormModal()),
    scope: 'search',
    dependencies: [loaded, isAllowedToEdit, saveSearch, currentTitle],
  });

  useHotkey({
    actionKey: 'save-as',
    callback: () => openFormModal(),
    scope: 'search',
  });

  return (
    <Container aria-label={t('search_meta.buttons')}>
      <SavedSearchForm
        key={currentTitle}
        show={showForm}
        saveSearch={saveSearch}
        saveAsSearch={saveAsSearch}
        isCreateNew={isNew || !isAllowedToEdit}
        toggleModal={toggleFormModal}
        value={currentTitle}
        selectedStreamGRN={selectedStreamsGRN}
        viewId={!isNew && view.id}>
        <SaveViewButton title={title} ref={formTarget} onClick={toggleFormModal} />
      </SavedSearchForm>
      <Button title={t('actions_menu.load_title')} onClick={toggleListModal}>
        <Icon name="folder" type="regular" /> {t('actions_menu.load')}
      </Button>
      {showList && (
        <SavedSearchesModal
          deleteSavedSearch={deleteSavedSearch}
          toggleModal={toggleListModal}
          activeSavedSearchId={view.id}
        />
      )}
      <ShareButton
        entityType="search"
        entityId={view.id}
        bsSize="md"
        onClick={toggleShareSearch}
        disabledInfo={isNew && t('actions_menu.share_disabled')}
      />
      <MoreActionsMenu aria-label={t('actions_menu.dropdown_aria_label')} id="search-actions-dropdown" pullRight solid>
        <MenuItem onSelect={toggleMetadataEdit} disabled={!isAllowedToEdit} icon="edit">
          {t('actions_menu.edit_metadata')}
        </MenuItem>
        <IfPermitted permissions="dashboards:create">
          <MenuItem onSelect={_loadAsDashboard} icon="dashboard">
            {t('actions_menu.export_to_dashboard')}
          </MenuItem>
        </IfPermitted>
        <MenuItem onSelect={toggleExport} icon="download">
          {t('actions_menu.export')}
        </MenuItem>
        <MenuItem disabled={disableReset} onSelect={loadNewView} icon="restart_alt">
          {t('actions_menu.reset_search')}
        </MenuItem>
        {pluggableActions.length ? (
          <>
            <MenuItem divider />
            {pluggableActions}
          </>
        ) : null}
      </MoreActionsMenu>
      {showExport && <ExportModal view={view} closeModal={toggleExport} />}
      {showMetadataEdit && (
        <ViewPropertiesModal
          show
          view={view}
          title={t('actions_menu.editing_modal_title')}
          submitButtonText={t('actions_menu.update_search')}
          onClose={toggleMetadataEdit}
          onSave={onUpdateView}
        />
      )}
      {showShareSearch && (
        <EntityShareModal
          entityId={view.id}
          entityType="search"
          entityTitle={view.title}
          description={t('actions_menu.share_description')}
          onClose={toggleShareSearch}
        />
      )}
      {pluggableActionModals}
    </Container>
  );
};

export default SearchActionsMenu;
