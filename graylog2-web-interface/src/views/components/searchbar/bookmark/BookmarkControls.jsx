// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import { browserHistory } from 'react-router';

import Routes from 'routing/Routes';
import { newDashboardsPath } from 'views/Constants';
import { Button, ButtonGroup } from 'components/graylog';
import { Icon } from 'components/common';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import UserNotification from 'util/UserNotification';
import { ViewStore, ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import type { ViewStoreState } from 'views/stores/ViewStore';
import connect from 'stores/connect';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';

import BookmarkForm from './BookmarkForm';
import BookmarkList from './BookmarkList';

type Props = {
  viewStoreState: ViewStoreState,
};

type State = {
  showForm: boolean,
  showList: boolean,
  newTitle: string,
};

class BookmarkControls extends React.Component<Props, State> {
  formTarget: any;

  static propTypes = {
    viewStoreState: PropTypes.object.isRequired,
  };

  constructor(props: Props) {
    super(props);

    const { viewStoreState } = props;
    const { view } = viewStoreState;

    this.state = {
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
      .catch(error => UserNotification.error(`Saving view failed: ${this._extractErrorMessage(error)}`, 'Error!'));
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
      .catch(error => UserNotification.error(`Saving view failed: ${this._extractErrorMessage(error)}`, 'Error!'));
  };

  loadBookmark = () => {
    this.toggleListModal();
  };

  deleteBookmark = (deletedView) => {
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
      .catch(error => UserNotification.error(`Deleting view failed: ${this._extractErrorMessage(error)}`, 'Error!'));
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

  static contextType = ViewLoaderContext;

  render() {
    const { showForm, showList, newTitle } = this.state;
    const { viewStoreState } = this.props;
    const { view, dirty } = viewStoreState;


    const bookmarkList = showList && (
      <BookmarkList loadBookmark={this.loadBookmark}
                    deleteBookmark={this.deleteBookmark}
                    toggleModal={this.toggleListModal} />
    );

    const loaded = (view && view.id);
    const bookmarkStyle = loaded ? 'bookmark' : 'bookmark-o';
    let bookmarkColor: string = '';
    if (loaded) {
      bookmarkColor = dirty ? '#ffc107' : '#007bff';
    }

    const disableReset = !(dirty || loaded);
    let title: string;
    if (dirty) {
      title = 'Unsaved changes';
    } else {
      title = loaded ? 'Saved search' : 'Save search';
    }

    const bookmarkForm = showForm && (
      <BookmarkForm onChangeTitle={this.onChangeTitle}
                    target={this.formTarget}
                    saveSearch={this.saveSearch}
                    saveAsSearch={this.saveAsSearch}
                    disableCreateNew={newTitle === view.title}
                    isCreateNew={!view.id}
                    toggleModal={this.toggleFormModal}
                    value={newTitle} />
    );

    return (
      <div className="pull-right">
        <ButtonGroup>
          <React.Fragment>
            <Button title="Export to new dashboard"
                    onClick={this.loadAsDashboard}>
              <Icon name="dashboard" />
            </Button>
            <Button disabled={disableReset}
                    title="Empty search"
                    onClick={() => {
                      browserHistory.push(Routes.SEARCH);
                    }}>
              <Icon name="eraser" />
            </Button>
            <Button title={title} ref={(elem) => { this.formTarget = elem; }} onClick={this.toggleFormModal}>
              <Icon style={{ color: bookmarkColor }} name={bookmarkStyle} />
            </Button>
            {bookmarkForm}
          </React.Fragment>
          <Button title="List of saved searches"
                  onClick={this.toggleListModal}>
            <Icon name="folder-o" />
          </Button>
          {bookmarkList}
        </ButtonGroup>
      </div>
    );
  }
}

export default connect(BookmarkControls, { viewStoreState: ViewStore });
