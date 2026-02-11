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
import React from 'react';

type Props = {
  message?: string;
  userId?: string;
};

const ExampleSidebarContent = ({ message = 'Test Sidebar Content', userId = undefined }: Props) => (
  <div>
    <h4>Example Sidebar</h4>
    <p>{message}</p>
    {userId && <p>User ID: {userId}</p>}
    <p>This is a test component to verify the sidebar functionality works correctly.</p>
  </div>
);

export default ExampleSidebarContent;
