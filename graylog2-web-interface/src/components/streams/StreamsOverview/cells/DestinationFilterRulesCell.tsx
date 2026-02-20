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
import { useRef, useCallback } from 'react';

import type { Stream } from 'stores/streams/StreamsStore';
import { CountBadge } from 'components/common';
import useStreamDestinationFilterRuleCount from 'components/streams/hooks/useStreamDestinationFilterRuleCount';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';

type Props = {
  stream: Stream;
};

const DestinationFilterRulesCell = ({ stream }: Props) => {
  const buttonRef = useRef();
  const { toggleSection, expandedSections } = useExpandedSections();
  const hasFilterRules = !stream.is_default && stream.is_editable;
  const { data: destinationFilterRuleCount } = useStreamDestinationFilterRuleCount(stream.id, hasFilterRules);
  const toggleFilterRulesSection = useCallback(
    () => toggleSection(stream.id, 'destination_filters'),
    [stream.id, toggleSection],
  );

  if (!hasFilterRules) {
    return null;
  }

  const destinationFilterRulesSectionIsOpen = expandedSections?.[stream.id]?.includes('destination_filters');

  return (
    <CountBadge
      count={destinationFilterRuleCount}
      onClick={toggleFilterRulesSection}
      ref={buttonRef}
      title={`${destinationFilterRulesSectionIsOpen ? 'Hide' : 'Show'} filter rules`}
    />
  );
};

export default DestinationFilterRulesCell;
