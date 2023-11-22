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
import PropTypes from 'prop-types';
import type { Location } from 'react-router-dom';
import { useBlocker } from 'react-router-dom';
import { useCallback, useEffect } from 'react';

import AppConfig from 'util/AppConfig';

/**
 * This component should be conditionally rendered if you have a form that is in a "dirty" state. It will confirm with the user that they want to navigate away, refresh, or in any way unload the component.
 */
type Props = {
  question: string,
};

const locationHasChanged = (currentLocation: Location, newLocation: Location, question: string) => ((newLocation.pathname !== currentLocation.pathname)
  // eslint-disable-next-line no-alert
  ? !window.confirm(question)
  : false);

const ConfirmLeaveDialog = ({ question }: Props) => {
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

  useBlocker((history) => !AppConfig.gl2DevMode() && locationHasChanged(history.currentLocation, history.nextLocation, question));

  return null;
};

ConfirmLeaveDialog.propTypes = {
  /** Phrase used in the confirmation dialog. */
  question: PropTypes.string,
};

ConfirmLeaveDialog.defaultProps = {
  question: 'Are you sure?',
};

/** @component */
export default ConfirmLeaveDialog;
