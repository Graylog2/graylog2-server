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
const modalBase = 1040;
const sidebar = 6;

const zIndices = {
  sidebar: sidebar,
  sidebarContentColumn: sidebar + 1,
  sidebarOverlay: sidebar - 1,
  modalOverlay: modalBase,
  modalBody: modalBase + 10,
  notifications: modalBase + 11,
} as const;
export default zIndices;
