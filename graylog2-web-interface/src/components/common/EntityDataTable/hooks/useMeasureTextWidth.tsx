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
import { useTheme } from 'styled-components';
import { useMemo, useState, useEffect, useCallback } from 'react';

const useMeasurementContext = (font: string) =>
  useMemo(() => {
    const measurementContext = document.createElement('canvas').getContext('2d');

    measurementContext.font = font;

    return measurementContext;
  }, [font]);

const useFontLoaded = (font: string) => {
  const [fontLoaded, setFontLoaded] = useState<boolean>(() => document.fonts.check(font));

  useEffect(() => {
    let isCancelled = false;

    if (document.fonts.check(font)) {
      setFontLoaded(true);
    } else {
      document.fonts
        .load(font)
        .then(() => {
          if (!isCancelled) {
            setFontLoaded(true);
          }
        })
        .catch(() => {
          if (!isCancelled) {
            setFontLoaded(true);
          }
        });
    }

    return () => {
      isCancelled = true;
    };
  }, [font]);

  return fontLoaded;
};

const useMeasureTextWidth = ({
  weight: weightProps,
  family: familyProp,
  size: sizeProp,
}: {
  weight?: number;
  family?: string; // family where multiple fonts are separated by commas
  size?: number; // size in px
}) => {
  const theme = useTheme();
  const family = familyProp ?? theme.fonts.family.body;
  const size = sizeProp ?? theme.fonts.size.root;
  const weight = weightProps ?? 400;

  const familyArray = family.split(',').map((font) => font.trim());

  const font = `${weight} ${size} ${familyArray[0]}`;
  const fallbackFont = `${weight} ${size} ${familyArray[1]}`;

  const fontLoaded = useFontLoaded(font);
  const measurementContext = useMeasurementContext(fontLoaded ? font : fallbackFont);

  return useCallback(
    (text: string) => {
      const metrics = measurementContext.measureText(text);

      return Math.ceil(metrics.width);
    },
    [measurementContext],
  );
};

export default useMeasureTextWidth;
