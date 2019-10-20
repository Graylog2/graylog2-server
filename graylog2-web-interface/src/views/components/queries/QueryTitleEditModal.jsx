// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import { Modal, Button } from 'components/graylog';
import Input from 'components/bootstrap/Input';

import type { TitlesMap } from 'views/stores/TitleTypes';

type Props = {
  onTitleChange: (queryId: string, newTitle: string) => Promise<TitlesMap>,
  selectedQueryId: string,
}

type State = {
  show: boolean,
  titleDraft: string,
}

class QueryTitleEditModal extends React.Component<Props, State> {
  static propTypes = {
    onTitleChange: PropTypes.func.isRequired,
    selectedQueryId: PropTypes.number.isRequired,
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
  }

  close = () => {
    this.setState({
      show: false,
      titleDraft: '',
    });
  }

  _onDraftSave = () => {
    const { titleDraft } = this.state;
    const { onTitleChange, selectedQueryId } = this.props;
    onTitleChange(selectedQueryId, titleDraft);
    this.close();
  };

  // eslint-disable-next-line no-undef
  _onDraftChange = (evt: SyntheticInputEvent<HTMLInputElement>) => {
    this.setState({ titleDraft: evt.target.value });
  };

  render() {
    const { show, titleDraft } = this.state;
    return (
      <Modal show={show} bsSize="large" onHide={this.close}>
        <Modal.Header closeButton>
          <Modal.Title>Edit query title</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Input autoFocus
                 help="The title of the query tab."
                 id="title"
                 label="Title"
                 name="title"
                 onChange={this._onDraftChange}
                 required
                 type="text"
                 value={titleDraft} />
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={this._onDraftSave} bsStyle="success">Save</Button>
          <Button onClick={this.close}>Cancel</Button>
        </Modal.Footer>
      </Modal>
    );
  }
}

export default QueryTitleEditModal;
