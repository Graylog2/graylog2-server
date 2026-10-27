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
import React, { useMemo, useState } from 'react';
import styled from 'styled-components';
import { useTranslation } from 'react-i18next';

import {
  ClipboardButton,
  Icon,
  SearchForm,
  IfPermitted,
  Timestamp,
  NoEntitiesExist,
  RelativeTime,
} from 'components/common';
import { Button, Panel, Table } from 'components/bootstrap';
import type { Token, TokenSummary } from 'hooks/useUsers';
import { sortByDate } from 'util/SortUtils';
import { Headline } from 'components/common/Section/SectionComponent';
import useCurrentUser from 'hooks/useCurrentUser';
import type { BasicUser } from 'hooks/useBasicUser';

import CreateTokenForm from './CreateTokenForm';
import TokenActions from './UsersTokenManagement/TokenManagementActions';

const StyledTokenPanel = styled(Panel)`
  &.panel {
    margin: 10px 0;
    background-color: ${(props) => props.theme.colors.global.contentBackground};

    .panel-heading {
      color: ${(props) => props.theme.colors.gray[30]};
    }
  }
`;

const StyledCopyTokenButton = styled(ClipboardButton)`
  vertical-align: baseline;
  margin-left: 1em;
`;

const StyledSearchForm = styled(SearchForm)`
  margin-bottom: 10px;
`;

type Props = {
  creatingToken?: boolean;
  onCreate: ({ tokenName, tokenTtl }: { tokenName: string; tokenTtl: string }) => Promise<Token>;
  tokens?: TokenSummary[];
  user: BasicUser;
  onDelete?: () => void;
};

const TokenList = ({ creatingToken = false, onCreate, user, onDelete = () => {}, tokens = [] }: Props) => {
  const { t } = useTranslation('users');
  const currentUser = useCurrentUser();
  const [createdToken, setCreatedToken] = useState<Token | undefined>();
  const [query, setQuery] = useState('');

  const effectiveTokens = useMemo(() => {
    const queryRegex = new RegExp(query, 'i');

    return tokens
      .filter(({ name }) => queryRegex.test(name))
      .sort((token1, token2) => sortByDate(token1.last_access, token2.last_access, 'desc'));
  }, [query, tokens]);

  const handleTokenCreation = async ({ tokenName, tokenTtl }: { tokenName: string; tokenTtl: string }) => {
    const token = await onCreate({ tokenName, tokenTtl });
    setCreatedToken(token);
  };
  const updateQuery = (nextQuery?: string) => setQuery(nextQuery || '');

  return (
    <>
      <IfPermitted permissions={[`users:tokencreate:${user.username}`]} anyPermissions>
        <Headline>{t('token_list.title')}</Headline>
        <CreateTokenForm
          onCreate={handleTokenCreation}
          creatingToken={creatingToken}
          forceDefaultTtl={user.serviceAccount ? 'P100Y' : undefined}
        />
      </IfPermitted>
      {createdToken && (
        <StyledTokenPanel bsStyle="success">
          <Panel.Heading>
            <Panel.Title>
              {t('token_list.token_created', { name: createdToken.name })}
            </Panel.Title>
          </Panel.Heading>
          <Panel.Body>
            <p>{t('token_list.token_created_description')}</p>
            <pre>
              {createdToken.token}
              <StyledCopyTokenButton title={<Icon name="content_copy" />} text={createdToken.token} bsSize="xsmall" />
            </pre>
            <Button bsStyle="primary" onClick={() => setCreatedToken(undefined)}>
              {t('token_list.done_button')}
            </Button>
          </Panel.Body>
        </StyledTokenPanel>
      )}
      <hr />

      <Headline>{t('token_list.title_list')}</Headline>
      <StyledSearchForm onSearch={updateQuery} onReset={updateQuery} label={t('token_list.filter_label')} useLoadingState={false} />
      {effectiveTokens.length === 0 ? (
        <NoEntitiesExist>{query === '' ? t('token_list.no_tokens_to_display') : t('token_list.no_tokens_match_filter')}</NoEntitiesExist>
      ) : (
        <Table bordered condensed>
          <thead>
            <tr>
              <th>{t('token_list.column_name')}</th>
              <th>{t('token_list.column_created')}</th>
              <th>{t('token_list.column_last_access')}</th>
              <th>{t('token_list.column_expires_at')}</th>
              <th className="text-right">{t('token_list.column_actions')}</th>
            </tr>
          </thead>
          <tbody>
            {effectiveTokens.map((token) => {
              const tokenNeverUsed = !token.last_access || Date.parse(token.last_access) === 0;

              return (
                <tr key={token.id}>
                  <td>{token.name}</td>
                  <td>
                    <Timestamp dateTime={token.created_at} />
                  </td>
                  <td>{tokenNeverUsed ? t('token_list.never_used') : <RelativeTime dateTime={token.last_access} />}</td>
                  <td>
                    <Timestamp dateTime={token.expires_at} />
                  </td>
                  <td>
                    <TokenActions
                      username={user.username}
                      userId={currentUser.id}
                      tokenId={token.id}
                      tokenName={token.name}
                      onDeleteCallback={onDelete}
                    />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </Table>
      )}
    </>
  );
};

export default TokenList;
