// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import { Modal, Button } from 'components/graylog';
import Input from 'components/bootstrap/Input';

import type { TitlesMap } from 'views/stores/TitleTypes';

/**
 * Component that allows the user to update a QueryTab title.
 * It takes the active query title as an argument on open and will use it as the draft.
 * The open action is getting called outside by referencing this component.
 */

type Props = {
  onTitleChange: (newTitle: string) => Promise<TitlesMap>,
}

type State = {
  show: boolean,
  titleDraft: string,
}

class QueryTitleEditModal extends React.Component<Props, State> {
  static propTypes = {
    onTitleChange: PropTypes.func.isRequired,
  };

  state = {
    show: false,
    titleDraft: '',
  };

  open = (activeQueryTitle: string) => {
    this.setState({
      show: true,
      titleDraft: activeQueryTitle,
    });
  };

  _close = () => {
    this.setState({
      show: false,
      titleDraft: '',
    });
  };

  _onDraftSave = () => {
    const { titleDraft } = this.state;
    const { onTitleChange } = this.props;
    if (titleDraft !== '') {
      onTitleChange(titleDraft);
      this._close();
    }
  };

  _onDraftChange = (evt: SyntheticInputEvent<HTMLInputElement>) => {
    this.setState({ titleDraft: evt.target.value });
  };

  render() {
    const { show, titleDraft } = this.state;
    return (
      <Modal show={show} bsSize="large" onHide={this._close}>
        <Modal.Header closeButton>
          <Modal.Title>Editing query title</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Input autoFocus
                 help="The title of the query tab. It has a maximum length of 40 characters."
                 id="title"
                 label="Title"
                 name="title"
                 onChange={this._onDraftChange}
                 maxLength={40}
                 type="text"
                 value={titleDraft} />
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={this._onDraftSave} bsStyle="success">Save</Button>
          <Button onClick={this._close}>Cancel</Button>
        </Modal.Footer>
      </Modal>
    );
  }
}

export default QueryTitleEditModal;
