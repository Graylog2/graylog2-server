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
// @flow strict
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { List } from 'immutable';

import { ClipboardButton, TableList, Spinner } from 'components/common';
import { Button, Checkbox, ButtonGroup } from 'components/graylog';
import type { Token } from 'actions/users/UsersActions';

import CreateTokenForm from './CreateTokenForm';

type Props = {
  creatingToken: boolean,
  deletingToken: ?string,
  onCreate: (tokenName: string) => void,
  onDelete: (tokenId: string, tokenName: string) => void,
  tokens: Token[],
};

const TokenList = ({ creatingToken, deletingToken, onCreate, onDelete, tokens }: Props) => {
  const [hideTokens, setHideTokens] = useState(true);

  const onShowTokensChanged = (event) => {
    setHideTokens(event.target.checked);
  };

  const deleteToken = (token) => {
    return () => {
      onDelete(token.id, token.name);
    };
  };

  const itemActionsFactory = (token) => {
    const deleteButton = deletingToken === token.id ? <Spinner text="Deleting..." /> : 'Delete';

    return (
      <ButtonGroup>
        <ClipboardButton title="Copy to clipboard" text={token.token} bsSize="xsmall" />
        <Button bsSize="xsmall"
                disabled={deletingToken === token.id}
                bsStyle="primary"
                onClick={deleteToken(token)}>
          {deleteButton}
        </Button>
      </ButtonGroup>
    );
  };

  return (
    <span>
      <CreateTokenForm onCreate={onCreate} creatingToken={creatingToken} />
      <TableList filterKeys={['name', 'token']}
                 items={List(tokens)}
                 idKey="token"
                 titleKey="name"
                 descriptionKey="token"
                 hideDescription={hideTokens}
                 enableBulkActions={false}
                 itemActionsFactory={itemActionsFactory} />
      <Checkbox id="hide-tokens" onChange={onShowTokensChanged} checked={hideTokens}>
        Hide Tokens
      </Checkbox>
    </span>
  );
};

TokenList.propTypes = {
  tokens: PropTypes.arrayOf(PropTypes.object),
  onDelete: PropTypes.func,
  onCreate: PropTypes.func,
  creatingToken: PropTypes.bool,
  deletingToken: PropTypes.string,
};

TokenList.defaultProps = {
  tokens: [],
  onDelete: () => {},
  onCreate: () => {},
  creatingToken: false,
  deletingToken: undefined,
};

export default TokenList;
