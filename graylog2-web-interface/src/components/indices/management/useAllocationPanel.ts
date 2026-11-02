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
import { StringParam, useQueryParam } from 'routing/QueryParams';

export const ALLOCATION_PARAM = 'allocation';
export const EXPLAIN_ALLOCATION = 'explain';

// Whether the shard allocation panel is open, kept in the URL so links (e.g. the cluster health summary's) can open it.
const useAllocationPanel = (): [boolean, (show: boolean) => void] => {
  const [allocation, setAllocation] = useQueryParam(ALLOCATION_PARAM, StringParam);

  return [allocation === EXPLAIN_ALLOCATION, (show: boolean) => setAllocation(show ? EXPLAIN_ALLOCATION : undefined)];
};

export default useAllocationPanel;
