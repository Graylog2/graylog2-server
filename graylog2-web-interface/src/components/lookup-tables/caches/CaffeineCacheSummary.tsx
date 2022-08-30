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

import { TimeUnit } from 'components/common';
import type { LookupTableCache } from 'logic/lookup-tables/types';

type Props = {
  cache: LookupTableCache,
};

const CaffeineCacheSummary = ({ cache }: Props) => {
  return (
    <dl>
      <dt>Maximum entries:</dt>
      <dd>{cache.config.max_size}</dd>

      <dt>Expire after access:</dt>
      <dd><TimeUnit value={cache.config.expire_after_access} unit={cache.config.expire_after_access_unit} /></dd>

      <dt>Expire after write:</dt>
      <dd><TimeUnit value={cache.config.expire_after_write} unit={cache.config.expire_after_write_unit} /></dd>
    </dl>
  );
};

export default CaffeineCacheSummary;
