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
import { useEffect, useRef } from 'react';
import styled from 'styled-components';

import { createPalette, renderPlasmaFrame } from './plasma';

const SCALE = 10;

const Canvas = styled.canvas`
  z-index: -1;
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
  image-rendering: pixelated;
`;

const prefersReducedMotion = () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;

type Props = {
  className?: string;
};

const PlasmaBackground = ({ className = undefined }: Props) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    const context = canvas?.getContext('2d');

    if (!canvas || !context) {
      return undefined;
    }

    const palette = createPalette();
    const animate = !prefersReducedMotion();
    let imageData: ImageData | undefined;
    let frameId: number | undefined;

    const draw = (time: number) => {
      const width = Math.max(1, Math.ceil(canvas.clientWidth / SCALE));
      const height = Math.max(1, Math.ceil(canvas.clientHeight / SCALE));

      if (!imageData || imageData.width !== width || imageData.height !== height) {
        canvas.width = width;
        canvas.height = height;
        imageData = context.createImageData(width, height);
      }

      renderPlasmaFrame(imageData, palette, time / 1000);
      context.putImageData(imageData, 0, 0);

      if (animate) {
        frameId = window.requestAnimationFrame(draw);
      }
    };

    // eslint-disable-next-line compat/compat
    frameId = window.requestAnimationFrame(draw);

    return () => {
      if (frameId !== undefined) {
        window.cancelAnimationFrame(frameId);
      }
    };
  }, []);

  return <Canvas ref={canvasRef} className={className} aria-hidden="true" />;
};

export default PlasmaBackground;
