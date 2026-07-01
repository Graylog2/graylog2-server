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
import styled, { css } from 'styled-components';

import { Alert, Button, ButtonToolbar, Label, Table } from 'components/bootstrap';
import type { OutdatedIndex } from 'components/indices/hooks/useOutdatedIndices';

import { ACTION_DEFINITIONS, getAvailableActions } from './outdatedIndexActions';
import type { ConfirmedAction } from './outdatedIndexActions';
import type { IndicesGroup } from './outdatedIndexGroups';

const ActionsToolbar = styled(ButtonToolbar)`
  justify-content: flex-end;
`;

const ScrollableTableWrapper = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.md};
    margin-bottom: ${theme.spacings.md};

    & > table {
      margin-bottom: 0;
      table-layout: fixed;
    }

    & thead,
    & tbody {
      display: block;
    }

    & thead tr,
    & tbody tr {
      display: table;
      width: 100%;
      table-layout: fixed;
    }

    & tbody {
      max-height: 300px;
      overflow-y: auto;
      scrollbar-gutter: stable;
    }

    & thead {
      scrollbar-gutter: stable;
      overflow-y: hidden;
    }

    & thead th {
      background-color: ${theme.colors.table.head.background};
    }

    & tr > *:nth-child(1) {
      width: 40%;
      text-align: left;
    }

    & tr > *:nth-child(2) {
      width: 30%;
      text-align: left;
    }

    & tr > *:nth-child(3) {
      width: 30%;
      text-align: right;
    }
  `,
);

const OutdatedIndexActions = ({
  index,
  onAction,
  canArchive,
}: {
  index: OutdatedIndex;
  onAction: (action: ConfirmedAction) => void;
  canArchive: boolean;
}) => {
  const actions = getAvailableActions(index, canArchive);

  return (
    <ActionsToolbar>
      {actions.map((action) => {
        const actionDefinition = ACTION_DEFINITIONS[action];

        return (
          <Button
            key={action}
            bsSize="xs"
            bsStyle={actionDefinition.buttonStyle}
            onClick={() => onAction({ action, index })}>
            {actionDefinition.buttonLabel}
          </Button>
        );
      })}
    </ActionsToolbar>
  );
};

const IndicesGroupTable = ({
  group,
  onAction,
  canArchive,
}: {
  group: IndicesGroup;
  onAction: (action: ConfirmedAction) => void;
  canArchive: boolean;
}) => {
  if (group.indices.length === 0) {
    return <Alert bsStyle="info">No outdated {group.shortLabel} indices.</Alert>;
  }

  return (
    <ScrollableTableWrapper>
      <Table condensed hover striped>
        <thead>
          <tr>
            <th>{group.indexLabel}</th>
            <th>OpenSearch version</th>
            <th aria-label="Actions" />
          </tr>
        </thead>
        <tbody>
          {group.indices.map((index) => (
            <tr key={index.index_name}>
              <td>
                {index.index_name}
                {index.warm_index && (
                  <>
                    &nbsp;
                    <Label bsStyle="gray" bsSize="xs">
                      warm
                    </Label>
                  </>
                )}
              </td>
              <td>{index.version || 'Unknown'}</td>
              <td>
                <OutdatedIndexActions index={index} onAction={onAction} canArchive={canArchive} />
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </ScrollableTableWrapper>
  );
};

export default IndicesGroupTable;
