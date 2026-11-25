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
import { useContext, useLayoutEffect } from 'react';

import { singleton } from 'logic/singleton';

// Called when a part of rendered Markdown starts rendering asynchronously, e.g. a diagram. It returns the function to
// call once that part is done, so e.g. a PDF export can wait for it.
export type TrackMarkdownRender = () => () => void;

const MarkdownRenderTracker = singleton('components.common.markdown.MarkdownRenderTracker', () =>
  React.createContext<TrackMarkdownRender | undefined>(undefined),
);

// Tells the tracker while the calling component is busy. A layout effect, so it registers before effects that wait for
// the whole Markdown to render run.
export const useTrackMarkdownRender = (busy: boolean) => {
  const track = useContext(MarkdownRenderTracker);

  useLayoutEffect(() => (busy && track ? track() : undefined), [busy, track]);
};

export default MarkdownRenderTracker;
