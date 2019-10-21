import {
  mix,
  lighten,
  darken,
  opposite,
} from './index';

const COLOR_HEX = '#AD0707';

describe('Theme Utilities', () => {
  it('mix', () => {
    const lightMix = mix(COLOR_HEX, -1);
    const darkMix = mix(COLOR_HEX, 1);

    expect(lightMix).toBe('#AE0909');
    expect(darkMix).toBe('#AB0707');
  });

  it('lighten', () => {
    const color = lighten(COLOR_HEX, 10);

    expect(color).toBe('#BE0808');
  });

  it('darken', () => {
    const color = darken(COLOR_HEX, 10);

    expect(color).toBe('#A50707');
  });

  it('opposite', () => {
    const color = opposite(COLOR_HEX);

    expect(color).toBe('#07ADAD');
  });
});
