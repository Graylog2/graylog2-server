import React from 'react';
import { withTheme } from 'styled-components';

import ColorSwatch from './ColorSwatch';

export const ColorLevelExample = withTheme(({ theme }) => {
  const { info, primary } = theme.colors.variant;

  return (
    <>
      <p>
        <ColorSwatch name="info -5" color={theme.utils.colorLevel(info, -5)} />
        <ColorSwatch name="info" color={info} />
        <ColorSwatch name="info +5" color={theme.utils.colorLevel(info, 5)} />
      </p>
      <p>
        <ColorSwatch name="primary -8" color={theme.utils.colorLevel(primary, -8)} />
        <ColorSwatch name="primary -2" color={theme.utils.colorLevel(primary, -2)} />
        <ColorSwatch name="primary" color={primary} />
        <ColorSwatch name="primary +2" color={theme.utils.colorLevel(primary, 2)} />
        <ColorSwatch name="primary +8" color={theme.utils.colorLevel(primary, 8)} />
      </p>
    </>
  );
});

export const ContrastingColorExample = withTheme(({ theme }) => {
  const { info, primary } = theme.colors.variant;
  const { textDefault } = theme.colors.global;

  return (
    <>
      <p>
        <ColorSwatch name="info AAA" color={theme.utils.contrastingColor(info)} />
        <ColorSwatch name="info" color={info} />
        <ColorSwatch name="info AA" color={theme.utils.contrastingColor(info, 'AA')} />
      </p>
      <p>
        <ColorSwatch name="textDefault AAALarge" color={theme.utils.contrastingColor(textDefault, 'AAALarge')} />
        <ColorSwatch name="textDefault AAA" color={theme.utils.contrastingColor(textDefault)} />
        <ColorSwatch name="textDefault" color={textDefault} />
        <ColorSwatch name="textDefault AALarge" color={theme.utils.contrastingColor(textDefault, 'AALarge')} />
        <ColorSwatch name="textDefault AA" color={theme.utils.contrastingColor(textDefault, 'AA')} />
      </p>
      <p>
        <ColorSwatch name="primary AAA" color={theme.utils.contrastingColor(primary)} />
        <ColorSwatch name="primary" color={primary} />
        <ColorSwatch name="primary AA" color={theme.utils.contrastingColor(primary, 'AA')} />
      </p>
    </>
  );
});

export const ReadableColorExample = withTheme(({ theme }) => {
  const { info, primary } = theme.colors.variant;
  const { textDefault } = theme.colors.global;

  return (

    <>
      <p>
        <ColorSwatch name="info" color={info} />
        <ColorSwatch name="info readableColor" color={theme.utils.readableColor(info)} />
      </p>
      <p>
        <ColorSwatch name="textDefault" color={textDefault} />
        <ColorSwatch name="textDefault readableColor" color={theme.utils.readableColor(textDefault)} />
      </p>
      <p>
        <ColorSwatch name="primary" color={primary} />
        <ColorSwatch name="primary readableColor" color={theme.utils.readableColor(primary)} />
      </p>
    </>
  );
});
