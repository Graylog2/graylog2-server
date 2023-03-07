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

import { useRef, useCallback } from 'react';
import * as React from 'react';

import type { Stream } from 'stores/streams/StreamsStore';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';
import { CountBadge } from 'components/common';

type Props = {
  stream: Stream
}

const StreamRuleCell = ({ stream }: Props) => {
  const buttonRef = useRef();
  const { toggleSection } = useExpandedSections();

  const toggleRulesSection = useCallback(() => toggleSection(stream.id, 'rules'), [stream.id, toggleSection]);

  if (stream.is_default) {
    return null;
  }

  return (
    <CountBadge onClick={toggleRulesSection} ref={buttonRef}>
      {stream.rules.length}
    </CountBadge>
  );
};

export default StreamRuleCell;
