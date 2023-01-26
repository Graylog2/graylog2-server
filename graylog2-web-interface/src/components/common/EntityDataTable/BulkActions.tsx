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
import { useCallback } from 'react';

import MenuItem from 'components/bootstrap/MenuItem';
import OverlayDropdownButton from 'components/common/OverlayDropdownButton';
import StringUtils from 'util/StringUtils';

const Container = styled.div`
  display: flex;
  align-items: center;
`;

const SelectedEntitiesAmount = styled.div`
  margin-left: 5px;
`;

type Props = {
  bulkActions?: (selectedEntities: Array<string>, setSelectedEntities: (streamIds: Array<string>) => void) => React.ReactNode
  selectedEntities: Array<string>,
  setSelectedEntities: React.Dispatch<React.SetStateAction<Array<string>>>

};

const BulkActions = ({ selectedEntities, setSelectedEntities, bulkActions }: Props) => {
  const cancelEntitySelection = useCallback(() => setSelectedEntities([]), [setSelectedEntities]);

  return (
    <Container>
      <OverlayDropdownButton title="Bulk actions"
                             disabled={!selectedEntities?.length}
                             bsSize="small"
                             dropdownZIndex={1000}>
        {bulkActions(selectedEntities, setSelectedEntities)}
        <MenuItem divider />
        <MenuItem onClick={cancelEntitySelection}>Cancel selection</MenuItem>
      </OverlayDropdownButton>
      {!!selectedEntities.length && (
        <SelectedEntitiesAmount>
          {selectedEntities.length} {StringUtils.pluralize(selectedEntities.length, 'item', 'items')} selected
        </SelectedEntitiesAmount>
      )}
    </Container>
  );
};

BulkActions.defaultProps = {
  bulkActions: undefined,
};

export default BulkActions;
