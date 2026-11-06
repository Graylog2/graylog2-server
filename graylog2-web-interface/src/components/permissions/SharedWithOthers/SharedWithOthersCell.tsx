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

import { Spinner } from 'components/common';
import ExpandedSectionCount from 'components/common/EntityDataTable/ExpandedSectionCount';

import useShareSummary from './useShareSummary';
import { formatShareCounts } from './shareCounts';
import { SHARED_WITH_OTHERS_ID } from './Constants';

type Props = {
  entityId: string;
  grn: string;
};

const SharedWithOthersCell = ({ entityId, grn }: Props) => {
  const { data, isInitialLoading, isError } = useShareSummary(grn);

  if (isInitialLoading) {
    return <Spinner size="xs" />;
  }

  if (isError || !data) {
    return null;
  }

  return (
    <ExpandedSectionCount entityId={entityId} section={SHARED_WITH_OTHERS_ID} label="grantees">
      {formatShareCounts(data)}
    </ExpandedSectionCount>
  );
};

export default SharedWithOthersCell;
