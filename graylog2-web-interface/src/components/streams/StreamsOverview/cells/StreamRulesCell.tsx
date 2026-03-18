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
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';
import { CountBadge } from 'components/common';

type Props = {
  stream: Stream;
};

const StreamRulesCell = ({ stream }: Props) => {
  const buttonRef = useRef();
  const { toggleSection, expandedSections } = useExpandedSections();

  const toggleRulesSection = useCallback(() => toggleSection(stream.id, 'rules'), [stream.id, toggleSection]);

  if (stream.is_default || !stream.is_editable) {
    return null;
  }

  const streamRulesCount = stream.rules.length;
  if (streamRulesCount === 0) {
    return null;
  }

  const streamRulesSectionIsOpen = expandedSections?.[stream.id]?.includes('rules');
  const streamRulesSectionTitle = `${streamRulesSectionIsOpen ? 'Hide' : 'Show'} stream rules`;

  return (
    <CountBadge
      count={streamRulesCount}
      iconName={streamRulesSectionIsOpen ? 'keyboard_arrow_up' : 'keyboard_arrow_down'}
      onClick={toggleRulesSection}
      ref={buttonRef}
      title={streamRulesSectionTitle}
    />
  );
};

export default StreamRulesCell;
