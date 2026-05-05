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
import useLocation from 'routing/useLocation';

const HEALTH_QUERY_PARAM = 'health';
const ON_VALUE = 'on';

/**
 * Returns whether the Health module should be shown on the System Overview page.
 * Default: hidden. Add `?health=on` to the URL to show it.
 */
const useHealthModuleVisible = (): boolean => {
  const { search } = useLocation();

  return new URLSearchParams(search).get(HEALTH_QUERY_PARAM) === ON_VALUE;
};

export default useHealthModuleVisible;
