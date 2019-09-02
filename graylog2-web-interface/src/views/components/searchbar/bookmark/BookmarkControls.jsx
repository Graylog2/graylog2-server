// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import { Button, ButtonGroup } from 'react-bootstrap';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import type { ViewLoaderContextType } from 'views/logic/ViewLoaderContext';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import UserNotification from 'util/UserNotification';
import { ViewStore, ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import type { ViewStoreState } from 'views/stores/ViewStore';
import connect from 'stores/connect';

import BookmarkForm from './BookmarkForm';
import BookmarkList from './BookmarkList';
import styles from './BookmarkControls.css';

type Props = {
  viewStoreState: ViewStoreState,
};

type State = {
  showForm: boolean,
  showList: boolean,
  newTitle: string,
};

class BookmarkControls extends React.Component<Props, State> {
  static contextType = ViewLoaderContext;

  static propTypes = {
    viewStoreState: PropTypes.object.isRequired,
  };

  formTarget: any;

  constructor(props: Props, context: ViewLoaderContextType) {
    super(props, context);

    const { viewStoreState } = props;
    const { view } = viewStoreState;

    this.state = {
      showForm: false,
      showList: false,
      newTitle: view.title || '',
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

  // eslint-disable-next-line no-undef
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
      .catch(error => UserNotification.error(`Saving view failed: ${error}`, 'Error!'));
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
      .then((savedView) => {
        const { loaderFunc } = this.context;
        loaderFunc(savedView.id);
      })
      .then(this.toggleFormModal)
      .then(() => UserNotification.success(`Saving view "${newView.title}" was successful!`, 'Success!'))
      .catch((error) => {
        const errMsg = (error
          && error.additional
          && error.additional.body
          && error.additional.body.message) ? error.additional.body.message : error;

        UserNotification.error(`Saving view failed: ${errMsg}`, 'Error!');
      });
  };

  loadBookmark = () => {
    this.toggleListModal();
  };

  deleteBookmark = (view) => {
    return ViewManagementActions.delete(view)
      .then(() => UserNotification.success(`Deleting view "${view.title}" was successful!`, 'Success!'))
      .then(ViewActions.create())
      .catch(error => UserNotification.error(`Deleting view failed: ${error}`, 'Error!'));
  };

  render() {
    const { showForm, showList, newTitle } = this.state;
    const { viewStoreState } = this.props;
    const { view } = viewStoreState;

    const bookmarkForm = showForm && (
      <BookmarkForm onChangeTitle={this.onChangeTitle}
                    target={this.formTarget}
                    saveSearch={this.saveSearch}
                    saveAsSearch={this.saveAsSearch}
                    hideSave={!view.id}
                    toggleModal={this.toggleFormModal}
                    value={newTitle} />
    );

    const bookmarkList = (
      <BookmarkList loadBookmark={this.loadBookmark}
                    deleteBookmark={this.deleteBookmark}
                    showModal={showList}
                    toggleModal={this.toggleListModal} />
    );

    return (
      <div className={`${styles.position} pull-right`}>
        <ButtonGroup>
          <Button title="New search" onClick={ViewActions.create}>
            <i className="fa fa-file-o" />
          </Button>
          <ViewLoaderContext.Consumer>
            {({ loadedView, dirty }) => {
              const bookmarkStyle = (loadedView && loadedView.id) ? 'fa-bookmark' : 'fa-bookmark-o';
              const bookmarkColor = dirty ? '#ffc107' : '#007bff';
              const title = dirty ? 'Unsaved changes' : 'Saved search';
              return (
                <Button title={title} ref={(elem) => { this.formTarget = elem; }} onClick={this.toggleFormModal}>
                  <i style={{ color: bookmarkColor }} className={`fa ${bookmarkStyle}`} />
                </Button>
              );
            }}
          </ViewLoaderContext.Consumer>
          {bookmarkForm}
          <Button title="List of saved searches"
                  onClick={this.toggleListModal}>
            <i className="fa fa-folder-o" />
          </Button>
          {bookmarkList}
        </ButtonGroup>
      </div>
    );
  }
}

export default connect(BookmarkControls, { viewStoreState: ViewStore });
