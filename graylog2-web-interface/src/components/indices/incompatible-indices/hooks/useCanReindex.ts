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
import useRunsWithDataNode from 'components/datanode/hooks/useRunsWithDataNode';

/**
 * Reindexing a system index needs the admin certificate that only a Data Node search backend accepts.
 * Unless we know for sure that this Graylog does not run against Data Nodes, we keep the action available.
 */
const useCanReindex = (): boolean => {
  const { data: runsWithDataNode } = useRunsWithDataNode();

  return runsWithDataNode !== false;
};

export default useCanReindex;
