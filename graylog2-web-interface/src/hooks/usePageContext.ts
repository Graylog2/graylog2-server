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
import usePluginEntities from 'hooks/usePluginEntities';
import type { PageContext } from 'views/types';

const noop = (_context: PageContext) => {};
const usePageContext = (context: PageContext) => {
  const useContextExtension = usePluginEntities('assistant.useContextExtension')[0] ?? noop;

  useContextExtension(context);
};

export default usePageContext;
