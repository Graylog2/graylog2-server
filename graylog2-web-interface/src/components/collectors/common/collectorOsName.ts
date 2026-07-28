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
import { OS_LABELS } from './Constants';

import type { CollectorInstanceView } from '../types';

/**
 * The collector's operating system, for display in detail views. Prefers the description the agent
 * reports (the most specific, e.g. "Ubuntu 22.04.3 LTS") and otherwise translates the bare `os`
 * value to a human name, keeping the raw value if it is one we do not know.
 */
const collectorOsName = (instance: CollectorInstanceView): string => {
  const description = instance.non_identifying_attributes?.['os.description'] as string | undefined;

  if (description) {
    return description;
  }

  if (instance.os) {
    return OS_LABELS[instance.os] ?? instance.os;
  }

  return 'Unknown';
};

export default collectorOsName;
