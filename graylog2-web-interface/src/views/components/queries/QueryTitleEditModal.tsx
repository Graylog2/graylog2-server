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
import React from 'react';
import PropTypes from 'prop-types';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import Input from 'components/bootstrap/Input';

/**
 * Component that allows the user to update a QueryTab title.
 * It takes the active query title as an argument on open and will use it as the draft.
 * The open action is getting called outside by referencing this component.
 */

type Props = {
  onTitleChange: (newTitle: string) => void,
};

type State = {
  titleDraft: string,
  showModal: boolean,
};

class QueryTitleEditModal extends React.Component<Props, State> {
  static propTypes = {
    onTitleChange: PropTypes.func.isRequired,
  };

  constructor(props: Props) {
    super(props);

    this.state = {
      showModal: false,
      titleDraft: '',
    };
  }

  // eslint-disable-next-line react/no-unused-class-component-methods
  open = (activeQueryTitle: string) => {
    this.setState({
      titleDraft: activeQueryTitle,
      showModal: true,
    });
  };

  close = () => {
    this.setState({ showModal: false });
  };

  _onDraftSave = () => {
    const { titleDraft } = this.state;
    const { onTitleChange } = this.props;

    onTitleChange(titleDraft);
    this.close();
  };

  _onDraftChange = (evt: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ titleDraft: evt.target.value });
  };

  render() {
    const { titleDraft, showModal } = this.state;

    return (
      <BootstrapModalForm show={showModal}
                          title="Editing dashboard page title"
                          onSubmitForm={this._onDraftSave}
                          onCancel={this.close}
                          submitButtonText="Update title"
                          bsSize="large">
        <Input autoFocus
               help="Enter a helpful dashboard page title. It has a maximum length of 40 characters."
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
