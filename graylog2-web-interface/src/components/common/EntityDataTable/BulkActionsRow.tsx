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
import { useTranslation } from 'react-i18next';
import styled from 'styled-components';

import useSelectedEntities from './hooks/useSelectedEntities';

const Container = styled.div`
  display: flex;
  align-items: center;
`;

const SelectedEntitiesAmount = styled.div`
  margin-left: 5px;
`;

type Props = {
  bulkActions: React.ReactNode;
  className?: string;
};

const BulkActionsRow = ({ bulkActions, className = undefined }: Props) => {
  const { t } = useTranslation('common');
  const { selectedEntities } = useSelectedEntities();

  return (
    <Container className={className}>
      {bulkActions}
      {!!selectedEntities.length && (
        <SelectedEntitiesAmount>
          {t('entity_table.selected_items', { count: selectedEntities.length })}
        </SelectedEntitiesAmount>
      )}
    </Container>
  );
};

export default BulkActionsRow;
