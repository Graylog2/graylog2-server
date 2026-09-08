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

export const toPngViaPlotly = (container: HTMLElement): Promise<string> =>
  import('views/custom-plotly').then(({ default: Plotly }) => {
    const plotDiv = container.querySelector<HTMLElement>('.js-plotly-plot');

    if (!plotDiv) return Promise.reject(new Error('Plotly graph element not found in container'));

    return Plotly.toImage(plotDiv, { format: 'png', width: plotDiv.offsetWidth, height: plotDiv.offsetHeight });
  });

export const toPngViaHtml2Canvas = (container: HTMLElement, options?: { useCORS?: boolean }): Promise<string> =>
  import('html2canvas').then(({ default: html2canvas }) =>
    html2canvas(container, options).then((canvas) => canvas.toDataURL('image/png')),
  );
