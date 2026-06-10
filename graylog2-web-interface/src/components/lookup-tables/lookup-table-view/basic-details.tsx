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
import * as React from 'react';
import { useCallback } from 'react';
import styled from 'styled-components';
import { useNavigate } from 'react-router-dom';

import Routes from 'routing/Routes';
import { Button } from 'components/bootstrap';
import { Col, Row, DataWell } from 'components/lookup-tables/layout-componets';
import type { LookupTable } from 'logic/lookup-tables/types';

export const Description = styled.span`
  color: ${({ theme }) => theme.colors.text.secondary};
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: break-word;
`;

type Props = {
  table: LookupTable;
  canEdit?: boolean;
};

function LookupTableDetails({ table, canEdit = false }: Props) {
  const navigate = useNavigate();

  const handleEdit = useCallback(() => {
    navigate(Routes.SYSTEM.LOOKUPTABLES.edit(table.name));
  }, [table, navigate]);

  return (
    <>
      <Col $gap="xs">
        <Row $align="flex-end" $justify="space-between">
          <h2>Description</h2>
          {canEdit && (
            <Button bsStyle="primary" bsSize="sm" onClick={handleEdit} name="edit_square">
              Edit
            </Button>
          )}
        </Row>
        <Description>{table.description}</Description>
      </Col>
      {(table.default_single_value || table.default_multi_value) && (
        <DataWell style={{ overflow: 'auto' }}>
          <Col $gap="xs">
            {table.default_single_value && (
              <Row>
                <span style={{ width: 208 }}>Default single value</span>
                <Row $gap="md">
                  <code>{table.default_single_value}</code>
                  <span>
                    <Description>({table.default_single_value_type.toLowerCase()})</Description>
                  </span>
                </Row>
              </Row>
            )}
            {table.default_multi_value && (
              <Row>
                <span style={{ width: 208 }}>Default multi value</span>
                <Row $gap="md">
                  <code>{table.default_multi_value}</code>
                  <span>
                    <Description>({table.default_multi_value_type.toLowerCase()})</Description>
                  </span>
                </Row>
              </Row>
            )}
          </Col>
        </DataWell>
      )}
    </>
  );
}

export default LookupTableDetails;
