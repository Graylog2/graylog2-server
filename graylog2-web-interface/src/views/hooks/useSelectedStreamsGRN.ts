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

import * as Immutable from "immutable";

import useQueryFilters from "views/logic/queries/useQueryFilters";
import {filtersToStreamSet} from "views/logic/queries/Query";
import {createGRN} from "logic/permissions/GRN";
import useCurrentQuery from "views/logic/queries/useCurrentQuery";

const useSelectedStreamsGRN = (): { selectedStreamsGRN: Array<string> } => {
  const queryFilters = useQueryFilters();
  const currentQuery = useCurrentQuery();
  const streams = filtersToStreamSet(queryFilters.get(currentQuery.id, Immutable.Map())).toJS();
  const selectedStreamsGRN = streams?.map((stream: string) => createGRN('stream', stream));

  return { selectedStreamsGRN }
}

export default useSelectedStreamsGRN;
