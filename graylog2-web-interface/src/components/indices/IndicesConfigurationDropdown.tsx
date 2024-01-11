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
import React, { useCallback } from 'react';

import useHistory from 'routing/useHistory';
import { ButtonGroup, DropdownButton, MenuItem } from 'components/bootstrap';
import Routes from 'routing/Routes';
import useHasTypeMappingPermission from 'hooks/useHasTypeMappingPermission';

const IndicesConfigurationDropdown = ({ indexSetId }: { indexSetId: string }) => {
  const hasMappingPermission = useHasTypeMappingPermission();
  const history = useHistory();
  const onShowFieldTypes = useCallback(() => {
    history.push(Routes.SYSTEM.INDEX_SETS.FIELD_TYPES(indexSetId));
  }, [history, indexSetId]);

  if (!hasMappingPermission) return null;

  return (
    <ButtonGroup>
      <DropdownButton bsStyle="info" title="Configuration" id="indices-configuration-actions" pullRight>
        {hasMappingPermission && <MenuItem onClick={onShowFieldTypes}>Configure index field types</MenuItem>}
      </DropdownButton>
    </ButtonGroup>
  );
};

export default IndicesConfigurationDropdown;
