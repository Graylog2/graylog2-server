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

import React from 'react';

import StreamRuleList from 'components/streamrules/StreamRuleList';
import type { Stream } from 'stores/streams/StreamsStore';
import { Pluralize } from 'components/common';

const verbalMatchingType = (matchingType: 'OR' | 'AND') => {
  switch (matchingType) {
    case 'OR':
      return 'at least one';
    case 'AND':
    default:
      return 'all';
  }
};

type Props = {
  stream: Stream
}

const ExpandedRulesSection = ({ stream }: Props) => (
  <>
    <p>
      Must match {verbalMatchingType(stream.matching_type)} of the {stream.rules.length} configured stream <Pluralize value={stream.rules.length} plural="rules" singular="rule" />.
    </p>
    <StreamRuleList stream={stream} />
  </>
);

export default ExpandedRulesSection;
