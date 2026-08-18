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
import { NAVBAR_GAP } from 'theme/constants';

// The navigation bar distributes its width between four regions: the brand, the navigation menu,
// badges and icons. All but the menu keep their size, so the space the menu may occupy is whatever
// they leave over.
const NAVBAR_REGIONS = 4;
const RESERVED_GAP = NAVBAR_GAP * (NAVBAR_REGIONS - 1);

// Until the menu has been measured again it stays at its uncollapsed width and overflows, so it is
// not debounced at all. `ResizeObserver` notifications are already batched per frame.
const MEASURE_DEBOUNCE_MS = 0;

/**
 * Decides whether the navigation menu still fits next to the other regions of the navigation bar.
 *
 * The returned refs must be attached to the navigation bar itself and to each of its regions. Only
 * the menu is measured at its natural width: it must not shrink, so that its measurement describes
 * how much room it wants rather than how much it was given.
 */
const useNavigationCollapse = () => {
  const navbarRef = useRef<HTMLElement>(null);
  const brandRef = useRef<HTMLDivElement>(null);
  // `NotificationBadge` renders a list, which is what this ref ends up attached to.
  const badgesRef = useRef<HTMLUListElement>(null);
  const iconsRef = useRef<HTMLElement>(null);

  // These four are always rendered, so a plain ref is enough to keep measuring them.
  const { width: navbarWidth } = useElementDimensions(navbarRef, MEASURE_DEBOUNCE_MS);
  const { width: brandWidth } = useElementDimensions(brandRef, MEASURE_DEBOUNCE_MS);
  const { width: badgesWidth } = useElementDimensions(badgesRef, MEASURE_DEBOUNCE_MS);
  const { width: iconsWidth } = useElementDimensions(iconsRef, MEASURE_DEBOUNCE_MS);
  // The menu is not: it is unmounted for as long as it is collapsed, which needs a measurement that
  // survives being unmounted and picks the menu up again when it returns.
  const [menuRef, menuWidth] = useNaturalWidth<HTMLUListElement>();

  const availableWidth = navbarWidth - brandWidth - badgesWidth - iconsWidth - RESERVED_GAP;
  // Before the first measurement every width is zero, so the menu starts out expanded and is
  // collapsed synchronously by the hook's layout effect if it does not fit.
  const collapsed = menuWidth > 0 && menuWidth > availableWidth;

  return { navbarRef, brandRef, badgesRef, iconsRef, menuRef, collapsed };
};

export default useNavigationCollapse;
