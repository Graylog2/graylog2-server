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
import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import { Button } from 'components/bootstrap';
import { ConfirmDialog, Icon } from 'components/common';

import type { StreamOutputFilterRule } from './Types';

import useStreamOutputRuleMutation from '../../hooks/useStreamOutputRuleMutation';

type Props ={
  streamId: string,
  filterOutputRule: StreamOutputFilterRule,
};

const FilterDeleteButton = ({ streamId, filterOutputRule }: Props) => {
  const [showDialog, setShowDialog] = useState(null);
  const { removeStreamOutputRule } = useStreamOutputRuleMutation();
  const queryClient = useQueryClient();

  return (
    <>
      <Button bsStyle="link"
              bsSize="xsmall"
              onClick={() => setShowDialog(true)}
              title="View">
        <Icon name="delete" type="regular" />
      </Button>
      {showDialog && (
      <ConfirmDialog title="Delete Rule"
                     show
                     onConfirm={async () => {
                       await removeStreamOutputRule({ streamId, filterId: filterOutputRule.id }).then(() => {
                         queryClient.invalidateQueries(['streams']);
                       });

                       setShowDialog(false);
                     }}
                     onCancel={() => setShowDialog(false)}>
        {`Are you sure you want to delete  ${filterOutputRule.title} rule ?`}
      </ConfirmDialog>
      )}
    </>
  );
};

export default FilterDeleteButton;
