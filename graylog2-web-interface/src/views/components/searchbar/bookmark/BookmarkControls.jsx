// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import { Button, ButtonGroup } from 'react-bootstrap';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import type { ViewLoaderContextType } from 'views/logic/ViewLoaderContext';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import UserNotification from 'util/UserNotification';
import { ViewStore } from 'views/stores/ViewStore';
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

    const newView = view.toBuilder()
      .title(newTitle)
      .type(View.Type.Search)
      .build();

    if (view.id) {
      ViewManagementActions.update(newView)
        .then(this.toggleFormModal)
        .then(() => UserNotification.success(`Saving view "${newView.title}" was successful!`, 'Success!'))
        .catch(error => UserNotification.error(`Saving view failed: ${error}`, 'Error!'));
      return;
    }
    ViewManagementActions.create(newView)
      .then(this.toggleFormModal)
      .then(() => UserNotification.success(`Saving view "${newView.title}" was successful!`, 'Success!'))
      .catch(error => UserNotification.error(`Saving view failed: ${error}`, 'Error!'));
  };

  loadBookmark = () => {
    this.toggleListModal();
  };

  render() {
    const { showForm, showList, newTitle } = this.state;

    const bookmarkForm = showForm && (
      <BookmarkForm onChangeTitle={this.onChangeTitle}
                    target={this.formTarget}
                    saveSearch={this.saveSearch}
                    toggleModal={this.toggleFormModal}
                    value={newTitle} />
    );

    const bookmarkList = (
      <BookmarkList loadBookmark={this.loadBookmark}
                    showModal={showList}
                    toggleModal={this.toggleListModal} />
    );

    return (
      <div className={`${styles.position} pull-right`}>
        <ButtonGroup>
          <ViewLoaderContext.Consumer>
            {({ loadedView, dirty }) => {
              const bookmarkStyle = loadedView ? 'fa-bookmark' : 'fa-bookmark-o';
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
                  onClick={this.toggleListModal} >
            <i className="fa fa-folder" />
          </Button>
          {bookmarkList}
        </ButtonGroup>
      </div>
    );
  }
}

export default connect(BookmarkControls, { viewStoreState: ViewStore });
