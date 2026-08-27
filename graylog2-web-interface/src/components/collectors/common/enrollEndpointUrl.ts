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
import { qualifyUrl } from 'util/URLUtils';

/**
 * The URL a collector enrolls against: the Graylog server URL without the API path. The
 * qualified server URL always points at the API — the server generates it as the external
 * base URI (including any app path prefix) plus the trailing `api/` segment (see
 * `AppConfigResource`) — and the collector figures out the API path itself, so enrolling
 * happens against everything before that segment.
 */
const enrollEndpointUrl = (): string =>
  qualifyUrl('')
    .replace(/\/api\/?$/, '')
    .replace(/\/+$/, '');

export default enrollEndpointUrl;
