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

import QueryHelper from 'components/common/QueryHelper';

const fieldMap = {
  pattern: 'The pattern of the grok pattern',
};

const queryExample = (
  <p>
    Find grok patterns containing COMMON in the pattern:<br />
    <kbd>pattern:COMMON</kbd><br />
  </p>
);
const GrokPatternQueryHelper = () => (
  <QueryHelper entityName="grok pattern" commonFields={['name']} fieldMap={fieldMap} example={queryExample} />
);

export default GrokPatternQueryHelper;
