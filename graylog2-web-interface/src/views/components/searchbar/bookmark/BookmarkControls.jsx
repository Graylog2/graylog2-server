// @flow strict
import React from 'react';
import { Button, ButtonGroup } from 'react-bootstrap';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';

import BookmarkForm from './BookmarkForm';
import BookmarkList from './BookmarkList';
import styles from './BookmarkControls.css';

type Props = {
  views: any,
};

type State = {
  showForm: boolean,
  showList: boolean,
  newTitle: string,
};

class BookmarkControls extends React.Component<Props, State> {
  formTarget: any;

  listTarget: any;

  constructor() {
    super();

    this.state = {
      showForm: false,
      showList: false,
      newTitle: '',
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
    this.toggleFormModal();
  };

  loadBookmark = () => {
    this.toggleListModal();
  };

  render() {
    const { views } = this.props;
    const { showForm, showList, newTitle } = this.state;

    const bookmarkForm = showForm && (
      <BookmarkForm onChangeTitle={this.onChangeTitle}
                    target={this.formTarget}
                    saveSearch={this.saveSearch}
                    toggleModal={this.toggleFormModal}
                    value={newTitle} />
    );

    const bookmarkList = showList && (
      <BookmarkList loadBookmark={this.loadBookmark}
                    toggleModal={this.toggleListModal}
                    bookmarks={views}
                    target={this.listTarget} />
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
          <Button onClick={this.toggleListModal} ref={(elem) => { this.listTarget = elem; }}>
            <i className="fa fa-folder" />
          </Button>
          {bookmarkList}
        </ButtonGroup>
      </div>
    );
  }
}

export default BookmarkControls;
