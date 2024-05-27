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
import * as React from 'react';
import { BrowserRouter as Router, Route, Navigate, Routes } from 'react-router-dom';

import PreflightLogin from './PreflightLogin';
import PreflightApp from './PreflightApp';
import useAuthStatus from './hooks/useAuthStatus';

const App = () => {
  const isAuthenticated = useAuthStatus();

  return (
    <Router>
      <Routes>
        <Route path="/login" element={isAuthenticated ? <Navigate to="/" replace /> : <PreflightLogin />} />
        <Route path="/" element={isAuthenticated ? <PreflightApp /> : <Navigate to="/login" replace />} />
        <Route path="*" element={isAuthenticated ? <Navigate to="/" replace /> : <Navigate to="/login" replace />} />
      </Routes>
    </Router>
  );
};

export default App;
