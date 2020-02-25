// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
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
  titleDraft: string,
}

class QueryTitleEditModal extends React.Component<Props, State> {
  modal: BootstrapModalForm = React.createRef();

  static propTypes = {
    onTitleChange: PropTypes.func.isRequired,
  };

  state = {
    titleDraft: '',
  };

  open = (activeQueryTitle: string) => {
    this.setState({
      titleDraft: activeQueryTitle,
    });
    this.modal.open();
  }

  _onDraftSave = () => {
    const { titleDraft } = this.state;
    const { onTitleChange } = this.props;
    onTitleChange(titleDraft);
    this.modal.close();
  };

  _onDraftChange = (evt: SyntheticInputEvent<HTMLInputElement>) => {
    this.setState({ titleDraft: evt.target.value });
  };

  render() {
    const { titleDraft } = this.state;
    return (
      <BootstrapModalForm ref={(modal) => { this.modal = modal; }}
                          title="Editing query title"
                          onSubmitForm={this._onDraftSave}
                          submitButtonText="Save"
                          bsSize="large">
        <Input autoFocus
               help="The title of the query tab. It has a maximum length of 40 characters."
               id="title"
               label="Title"
               name="title"
               onChange={this._onDraftChange}
               maxLength={40}
               required
               type="text"
               value={titleDraft} />
      </BootstrapModalForm>
    );
  }
}

export default QueryTitleEditModal;
