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
import styled from 'styled-components';

import StringUtils from 'util/StringUtils';

import useSelectedEntities from './hooks/useSelectedEntities';

const Container = styled.div`
  display: flex;
  align-items: center;
`;

const SelectedEntitiesAmount = styled.div`
  margin-left: 5px;
`;

type Props = {
  bulkActions: React.ReactNode,
};

const BulkActionsRow = ({ bulkActions }: Props) => {
  const { selectedEntities } = useSelectedEntities();

  return (
    <Container>
      {bulkActions}
      {!!selectedEntities.length && (
        <SelectedEntitiesAmount>
          {selectedEntities.length} {StringUtils.pluralize(selectedEntities.length, 'item', 'items')} selected
        </SelectedEntitiesAmount>
      )}
    </Container>
  );
};

export default BulkActionsRow;
