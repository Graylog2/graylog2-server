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

import type { Stream } from 'logic/streams/types';
import useStreamDestinationFilterRuleCount from 'components/streams/hooks/useStreamDestinationFilterRuleCount';
import ExpandedSectionCount from 'components/common/EntityDataTable/ExpandedSectionCount';

type Props = {
  stream: Stream;
};

const DestinationFilterRulesCell = ({ stream }: Props) => {
  const hasFilterRules = !stream.is_default && stream.is_editable;
  const { data: destinationFilterRuleCount } = useStreamDestinationFilterRuleCount(stream.id, hasFilterRules);

  if (!hasFilterRules) {
    return null;
  }

  const filterRuleCount = destinationFilterRuleCount ?? 0;
  if (filterRuleCount === 0) {
    return null;
  }

  return (
    <ExpandedSectionCount
      entityId={stream.id}
      section="destination_filters"
      count={filterRuleCount}
      label="filter rules"
    />
  );
};

export default DestinationFilterRulesCell;
