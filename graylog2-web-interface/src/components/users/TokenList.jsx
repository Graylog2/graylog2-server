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
import PropTypes from 'prop-types';
import React from 'react';
import Immutable from 'immutable';

import ClipboardButton from 'components/common/ClipboardButton';
import { Button, Checkbox, ButtonGroup } from 'components/graylog';
import TableList from 'components/common/TableList';
import Spinner from 'components/common/Spinner';

import CreateTokenForm from './CreateTokenForm';

class TokenList extends React.Component {
  static propTypes = {
    tokens: PropTypes.arrayOf(PropTypes.object),
    onDelete: PropTypes.func,
    onCreate: PropTypes.func,
    creatingToken: PropTypes.bool,
    deletingToken: PropTypes.string,
  };

  static defaultProps = {
    tokens: [],
    onDelete: () => {},
    onCreate: () => {},
    creatingToken: false,
    deletingToken: undefined,
  };

  constructor(props) {
    super(props);

    this.state = {
      hide_tokens: true,
    };

    this._onShowTokensChanged = this._onShowTokensChanged.bind(this);
    this.itemActionsFactory = this.itemActionsFactory.bind(this);
  }

  _onShowTokensChanged(event) {
    this.setState({ hide_tokens: event.target.checked });
  }

  _deleteToken(token) {
    return () => {
      this.props.onDelete(token.id, token.name);
    };
  }

  itemActionsFactory(token) {
    const deleteButton = this.props.deletingToken === token.id ? <Spinner text="Deleting..." /> : 'Delete';

    return (
      <ButtonGroup>
        <ClipboardButton title="Copy to clipboard" text={token.token} bsSize="xsmall" />
        <Button bsSize="xsmall"
                disabled={this.props.deletingToken === token.id}
                bsStyle="primary"
                onClick={this._deleteToken(token)}>
          {deleteButton}
        </Button>
      </ButtonGroup>
    );
  }

  render() {
    const { creatingToken, onCreate, tokens } = this.props;
    const { hide_tokens: hideTokens } = this.state;

    return (
      <span>
        <CreateTokenForm onCreate={onCreate} creatingToken={creatingToken} />
        <TableList filterKeys={['name', 'token']}
                   items={Immutable.List(tokens)}
                   idKey="token"
                   titleKey="name"
                   descriptionKey="token"
                   hideDescription={hideTokens}
                   enableBulkActions={false}
                   itemActionsFactory={this.itemActionsFactory} />
        <Checkbox id="hide-tokens" onChange={this._onShowTokensChanged} checked={hideTokens}>
          Hide Tokens
        </Checkbox>
      </span>
    );
  }
}

export default TokenList;
