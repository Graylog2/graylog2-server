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

import { useState, useEffect } from 'react';

const useIsKeyHeld = (buttonKey: string) => {
  const [shiftHeld, setShiftHeld] = useState(false);

  useEffect(() => {
    function downHandler({ key }) {
      if (key === buttonKey) {
        setShiftHeld(true);
      }
    }

    function upHandler({ key }) {
      if (key === buttonKey) {
        setShiftHeld(false);
      }
    }

    window.addEventListener('keydown', downHandler);
    window.addEventListener('keyup', upHandler);

    return () => {
      window.removeEventListener('keydown', downHandler);
      window.removeEventListener('keyup', upHandler);
    };
  }, [setShiftHeld, buttonKey]);

  return shiftHeld;
};

export default useIsKeyHeld;
