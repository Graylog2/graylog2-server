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
import { useRef } from 'react';

import useElementDimensions from 'hooks/useElementDimensions';
import useNaturalWidth from 'components/navigation/useNaturalWidth';

const MEASURE_DEBOUNCE_MS = 0;

const useNavigationCollapse = () => {
  const navbarRef = useRef<HTMLElement>(null);
  const brandRef = useRef<HTMLDivElement>(null);
  const badgesRef = useRef<HTMLDivElement>(null);
  const iconsRef = useRef<HTMLElement>(null);

  const { width: navbarWidth } = useElementDimensions(navbarRef, MEASURE_DEBOUNCE_MS);
  const { width: brandWidth } = useElementDimensions(brandRef, MEASURE_DEBOUNCE_MS);
  const { width: badgesWidth } = useElementDimensions(badgesRef, MEASURE_DEBOUNCE_MS);
  const { width: iconsWidth } = useElementDimensions(iconsRef, MEASURE_DEBOUNCE_MS);
  const [menuRef, menuWidth] = useNaturalWidth<HTMLUListElement>();

  const availableWidth = navbarWidth - brandWidth - badgesWidth - iconsWidth;
  const collapsed = menuWidth > 0 && menuWidth > availableWidth;

  return { navbarRef, brandRef, badgesRef, iconsRef, menuRef, collapsed };
};

export default useNavigationCollapse;
