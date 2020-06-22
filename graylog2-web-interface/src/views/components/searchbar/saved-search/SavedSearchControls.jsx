// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { withTheme } from 'styled-components';
import { browserHistory } from 'react-router';

import connect from 'stores/connect';
import { type ThemeInterface } from 'theme';
import Routes from 'routing/Routes';
import { isPermitted } from 'util/PermissionsMixin';
import { newDashboardsPath } from 'views/Constants';
import { Button, ButtonGroup, DropdownButton, MenuItem } from 'components/graylog';
import { Icon } from 'components/common';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import UserNotification from 'util/UserNotification';
import { ViewStore, ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import type { ViewStoreState } from 'views/stores/ViewStore';
import onSaveView from 'views/logic/views/OnSaveViewAction';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import CSVExportModal from 'views/components/searchbar/csvexport/CSVExportModal';
import ShareViewModal from 'views/components/views/ShareViewModal';
import CurrentUserContext from 'contexts/CurrentUserContext';
import * as Permissions from 'views/Permissions';
import type { User } from 'stores/users/UsersStore';
import ViewPropertiesModal from 'views/components/views/ViewPropertiesModal';

import SavedSearchForm from './SavedSearchForm';
import SavedSearchList from './SavedSearchList';

type Props = {
  viewStoreState: ViewStoreState,
  theme: ThemeInterface,
};

type State = {
  showForm: boolean,
  showList: boolean,
  showCSVExport: boolean,
  showShareSearch: boolean,
  showMetadataEdit: boolean,
  newTitle: string,
};

const _isAllowedToEdit = (view: View, currentUser: ?User) => (
  view.owner === currentUser?.username
  || isPermitted(currentUser?.permissions, [Permissions.View.Edit(view.id)])
);

class SavedSearchControls extends React.Component<Props, State> {
  static propTypes = {
    viewStoreState: PropTypes.object.isRequired,
    theme: PropTypes.shape({
      colors: PropTypes.object,
    }).isRequired,
  };

  static contextType = ViewLoaderContext;

  formTarget: { current: null | Button };

  constructor(props: Props) {
    super(props);

    this.formTarget = React.createRef();

    const { viewStoreState } = props;
    const { view } = viewStoreState;

    this.state = {
      showMetadataEdit: false,
      showCSVExport: false,
      showShareSearch: false,
      showForm: false,
      showList: false,
      newTitle: (view && view.title) || '',
    };
  }

  toggleFormModal = () => {
    const { showForm } = this.state;
    this.setState({ showForm: !showForm });
  };

  toggleListModal = () => {
    const { showList } = this.state;
    this.setState({ showList: !showList });
  };

  toggleCSVExport = () => {
    const { showCSVExport } = this.state;
    this.setState({ showCSVExport: !showCSVExport });
  };

  toggleMetadataEdit = () => {
    const { showMetadataEdit } = this.state;
    this.setState({ showMetadataEdit: !showMetadataEdit });
  };

  toggleShareSearch = () => {
    const { showShareSearch } = this.state;
    this.setState({ showShareSearch: !showShareSearch });
  };

  onChangeTitle = (e: SyntheticInputEvent<HTMLInputElement>) => {
    this.setState({ newTitle: e.target.value });
  };

  saveSearch = () => {
    const { newTitle } = this.state;
    const { viewStoreState } = this.props;
    const { view } = viewStoreState;

    if (!view.id) {
      return;
    }

    const newView = view.toBuilder()
      .title(newTitle)
      .type(View.Type.Search)
      .build();

    ViewManagementActions.update(newView)
      .then(this.toggleFormModal)
      .then(() => UserNotification.success(`Saving view "${newView.title}" was successful!`, 'Success!'))
      .catch((error) => UserNotification.error(`Saving view failed: ${this._extractErrorMessage(error)}`, 'Error!'));
  };

  _extractErrorMessage = (error) => {
    return (error
      && error.additional
      && error.additional.body
      && error.additional.body.message) ? error.additional.body.message : error;
  };

  saveAsSearch = () => {
    const { newTitle } = this.state;
    const { viewStoreState } = this.props;
    const { view } = viewStoreState;

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
        const loaderFunc = this.context;
        loaderFunc(createdView.id).then(() => {
          browserHistory.push(Routes.pluginRoute('SEARCH_VIEWID')(createdView.id));
        });
      })
      .then(this.toggleFormModal)
      .then(() => UserNotification.success(`Saving view "${newView.title}" was successful!`, 'Success!'))
      .catch((error) => UserNotification.error(`Saving view failed: ${this._extractErrorMessage(error)}`, 'Error!'));
  };

  loadSavedSearch = () => {
    this.toggleListModal();
  };

  deleteSavedSearch = (deletedView) => {
    const { viewStoreState } = this.props;
    const { view } = viewStoreState;
    return ViewManagementActions.delete(deletedView)
      .then(() => UserNotification.success(`Deleting view "${deletedView.title}" was successful!`, 'Success!'))
      .then(() => ViewActions.create(View.Type.Search))
      .then(() => {
        if (deletedView.id === view.id) {
          browserHistory.push(Routes.SEARCH);
        }
      })
      .catch((error) => UserNotification.error(`Deleting view failed: ${this._extractErrorMessage(error)}`, 'Error!'));
  };

  loadAsDashboard = () => {
    const { viewStoreState } = this.props;
    const { view } = viewStoreState;

    browserHistory.push({
      pathname: newDashboardsPath,
      state: {
        view: view,
      },
    });
  };

  render() {
    const { showForm, showList, newTitle, showCSVExport, showShareSearch, showMetadataEdit } = this.state;
    const { viewStoreState: { view, dirty }, theme } = this.props;

    const loaded = (view && view.id);
    let savedSearchColor: string = '';
    if (loaded) {
      savedSearchColor = dirty ? theme.colors.variant.warning : theme.colors.variant.info;
    }

    const disableReset = !(dirty || loaded);
    let title: string;
    if (dirty) {
      title = 'Unsaved changes';
    } else {
      title = loaded ? 'Saved search' : 'Save search';
    }

    return (
      <CurrentUserContext.Consumer>
        {(currentUser) => {
          const isAllowedToEdit = (view && view.id) && _isAllowedToEdit(view, currentUser);
          return (
            <NewViewLoaderContext.Consumer>
              {(loadNewView) => (
                <div className="pull-right">
                  <ButtonGroup>
                    <>
                      <Button title={title} ref={this.formTarget} onClick={this.toggleFormModal}>
                        <Icon style={{ color: savedSearchColor }} name="star" type={loaded ? 'solid' : 'regular'} /> Save
                      </Button>
                      {showForm && (
                      <SavedSearchForm onChangeTitle={this.onChangeTitle}
                                       target={this.formTarget.current}
                                       saveSearch={this.saveSearch}
                                       saveAsSearch={this.saveAsSearch}
                                       disableCreateNew={newTitle === view.title}
                                       isCreateNew={!view.id}
                                       toggleModal={this.toggleFormModal}
                                       value={newTitle} />
                      )}
                    </>
                    <Button title="Load a previously saved search"
                            onClick={this.toggleListModal}>
                      <Icon name="folder" type="regular" /> Load
                    </Button>
                    {showList && (
                      <SavedSearchList loadSavedSearch={this.loadSavedSearch}
                                       deleteSavedSearch={this.deleteSavedSearch}
                                       toggleModal={this.toggleListModal} />
                    )}
                    <DropdownButton title={<Icon name="ellipsis-h" />} id="search-actions-dropdown" pullRight noCaret>
                      <MenuItem onSelect={this.toggleMetadataEdit} disabled={!isAllowedToEdit}>
                        <Icon name="edit" /> Edit metadata
                      </MenuItem>
                      <MenuItem onSelect={this.loadAsDashboard}><Icon name="dashboard" /> Export to dashboard</MenuItem>
                      <MenuItem onSelect={this.toggleCSVExport}><Icon name="cloud-download" /> Export to CSV</MenuItem>
                      <MenuItem disabled={disableReset} onSelect={() => loadNewView()} data-testid="reset-search">
                        <Icon name="eraser" /> Reset search
                      </MenuItem>
                      <MenuItem divider />
                      <MenuItem onSelect={this.toggleShareSearch} title="Share search" disabled={!isAllowedToEdit}>
                        <Icon name="share-alt" /> Share
                      </MenuItem>
                    </DropdownButton>
                    {showCSVExport && (
                      <CSVExportModal view={view} closeModal={this.toggleCSVExport} />
                    )}
                    {showMetadataEdit && (
                      <ViewPropertiesModal show
                                           view={view}
                                           title="Editing saved search"
                                           onClose={this.toggleMetadataEdit}
                                           onSave={onSaveView} />
                    )}
                    {showShareSearch && (
                      <ShareViewModal show view={view} onClose={this.toggleShareSearch} currentUser={currentUser} />
                    )}
                  </ButtonGroup>
                </div>
              )}
            </NewViewLoaderContext.Consumer>
          );
        }}
      </CurrentUserContext.Consumer>
    );
  }
}

export default connect(
  withTheme(SavedSearchControls),
  { viewStoreState: ViewStore },
  ({ viewStoreState }) => ({ viewStoreState }),
);
