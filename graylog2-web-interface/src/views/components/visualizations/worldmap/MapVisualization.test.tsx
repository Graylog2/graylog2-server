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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import MapVisualization from './MapVisualization';

// Mock react-leaflet components
jest.mock('react-leaflet', () => {
  const mockReact = require('react');

  return {
    MapContainer: mockReact.forwardRef(({ children, whenReady, id, style }: any, ref: any) => {
      mockReact.useEffect(() => {
        if (whenReady) {
          whenReady();
        }
      }, [whenReady]);

      return mockReact.createElement('div', { 'data-testid': 'map-container', id, style, ref }, children);
    }),
    TileLayer: ({ eventHandlers, url, attribution }: any) => {
      const handleTileError = () => {
        if (eventHandlers?.tileerror) {
          eventHandlers.tileerror();
        }
      };

      return mockReact.createElement(
        'div',
        { 'data-testid': 'tile-layer', 'data-url': url, 'data-attribution': attribution },
        mockReact.createElement(
          'button',
          { 'data-testid': 'trigger-tile-error', onClick: handleTileError },
          'Trigger Tile Error',
        ),
      );
    },
    CircleMarker: ({ children }: any) => mockReact.createElement('div', { 'data-testid': 'circle-marker' }, children),
    Popup: ({ children }: any) => mockReact.createElement('div', { 'data-testid': 'popup' }, children),
    useMap: () => ({
      setView: jest.fn(),
      getCenter: () => ({ lat: 0, lng: 0 }),
      getZoom: () => 1,
    }),
    useMapEvents: () => null,
  };
});

// Mock leaflet styles
jest.mock('leaflet/dist/leaflet.css', () => ({
  use: jest.fn(),
  unuse: jest.fn(),
}));

// Mock InteractiveContext
jest.mock('../../contexts/InteractiveContext', () => ({
  __esModule: true,
  default: {
    Consumer: ({ children }: any) => children(true),
  },
}));

describe('MapVisualization', () => {
  const defaultProps = {
    id: 'test-map',
    data: [],
    height: 400,
    width: 600,
    onChange: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render with default openstreetmap.us URL', () => {
    render(<MapVisualization {...defaultProps} />);

    const tileLayer = screen.getByTestId('tile-layer');

    expect(tileLayer).toHaveAttribute('data-url', 'https://{s}.tile.openstreetmap.us/{z}/{x}/{y}.png');
  });

  it('should cascade through fallbacks: us -> de -> org', async () => {
    render(<MapVisualization {...defaultProps} />);

    const triggerButton = screen.getByTestId('trigger-tile-error');

    // Initial URL should be .us
    let tileLayer = screen.getByTestId('tile-layer');
    expect(tileLayer).toHaveAttribute('data-url', 'https://{s}.tile.openstreetmap.us/{z}/{x}/{y}.png');

    // First error - should fallback to .de
    await userEvent.click(triggerButton);

    await waitFor(() => {
      tileLayer = screen.getByTestId('tile-layer');
      expect(tileLayer).toHaveAttribute('data-url', 'https://{s}.tile.openstreetmap.de/{z}/{x}/{y}.png');
    });

    // Second error - should fallback to .org
    await userEvent.click(triggerButton);

    await waitFor(() => {
      tileLayer = screen.getByTestId('tile-layer');
      expect(tileLayer).toHaveAttribute('data-url', 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png');
    });

    // Third error - should stay on .org (no more fallbacks)
    await userEvent.click(triggerButton);

    await waitFor(() => {
      tileLayer = screen.getByTestId('tile-layer');
      expect(tileLayer).toHaveAttribute('data-url', 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png');
    });
  });

  it('should use custom URL when provided', () => {
    const customUrl = 'https://custom.tile.server/{z}/{x}/{y}.png';
    render(<MapVisualization {...defaultProps} url={customUrl} />);

    const tileLayer = screen.getByTestId('tile-layer');

    expect(tileLayer).toHaveAttribute('data-url', customUrl);
  });

  it('should fallback from custom URL through de to org on errors', async () => {
    const customUrl = 'https://custom.tile.server/{z}/{x}/{y}.png';
    render(<MapVisualization {...defaultProps} url={customUrl} />);

    const triggerButton = screen.getByTestId('trigger-tile-error');

    // First error - should fallback to .de
    await userEvent.click(triggerButton);

    await waitFor(() => {
      const tileLayer = screen.getByTestId('tile-layer');
      expect(tileLayer).toHaveAttribute('data-url', 'https://{s}.tile.openstreetmap.de/{z}/{x}/{y}.png');
    });

    // Second error - should fallback to .org
    await userEvent.click(triggerButton);

    await waitFor(() => {
      const tileLayer = screen.getByTestId('tile-layer');
      expect(tileLayer).toHaveAttribute('data-url', 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png');
    });
  });

  it('should render markers from data', () => {
    const dataWithMarkers = [
      {
        keys: [{ field1: 'value1' }],
        name: 'Test Location',
        values: { '51.5074,-0.1278': 100 },
      },
    ];

    render(<MapVisualization {...defaultProps} data={dataWithMarkers} />);

    const markers = screen.getAllByTestId('circle-marker');

    expect(markers).toHaveLength(1);
  });

  it('should display error message when all fallbacks fail', async () => {
    render(<MapVisualization {...defaultProps} />);

    const triggerButton = screen.getByTestId('trigger-tile-error');

    // Trigger all fallbacks: .us -> .de -> .org -> all failed
    await userEvent.click(triggerButton); // us fails, fallback to de
    await userEvent.click(triggerButton); // de fails, fallback to org
    await userEvent.click(triggerButton); // org fails, show error

    await waitFor(() => {
      expect(screen.getByText('Unable to Load Map')).toBeInTheDocument();
      expect(
        screen.getByText(/All tile servers are currently unreachable/i),
      ).toBeInTheDocument();
    });
  });
});
