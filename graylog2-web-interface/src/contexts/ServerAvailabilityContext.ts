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
import { createContext } from 'react';

export type ServerError = {
  message: string;
  originalError: {
    method: string;
    url: string;
    status: number;
  };
};

export type ServerAvailabilityContextType = {
  server: { up: true } | { up: false; error: ServerError };
  version: string | undefined;
  ping: () => Promise<void>;
};

const ServerAvailabilityContext = createContext<ServerAvailabilityContextType | undefined>(undefined);

export default ServerAvailabilityContext;
