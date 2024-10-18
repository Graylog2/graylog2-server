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
import type { Location } from 'react-router-dom';
import { useBlocker } from 'react-router-dom';
import { useCallback, useEffect } from 'react';

import AppConfig from 'util/AppConfig';

/**
 * This component should be conditionally rendered if you have a form that is in a "dirty" state. It will confirm with the user that they want to navigate away, refresh, or in any way unload the component.
 * The `ignoredRoutes` prop is an array of routes that should not trigger the confirmation dialog.
 */
type Props = {
  question?: string,
  ignoredRoutes?: Array<string>,
};

const locationHasChanged = (currentLocation: Location, newLocation: Location, question: string, ignoredRoutes: Array<string>) => (
  (newLocation.pathname !== currentLocation.pathname && !ignoredRoutes.includes(newLocation.pathname))
    // eslint-disable-next-line no-alert
    ? !window.confirm(question)
    : false);

const ConfirmLeaveDialog = ({ question = 'Are you sure?', ignoredRoutes = [] }: Props) => {
  const handleLeavePage = useCallback((e) => {
    if (AppConfig.gl2DevMode()) {
      return null;
    }

    e.returnValue = question;

    return question;
  }, [question]);

  useEffect(() => {
    window.addEventListener('beforeunload', handleLeavePage);

    return () => {
      window.removeEventListener('beforeunload', handleLeavePage);
    };
  }, [handleLeavePage]);

  useBlocker((history) => !AppConfig.gl2DevMode() && locationHasChanged(history.currentLocation, history.nextLocation, question, ignoredRoutes));

  return null;
};

/** @component */
export default ConfirmLeaveDialog;
