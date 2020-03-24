import contrastingColor from './contrastingColor';

describe('contrastingColor', () => {
  it('should return a properly contrasting color', () => {
    const color1 = contrastingColor('#000');
    const color2 = contrastingColor('#fff');
    const color3 = contrastingColor('#f0f');
    const color4 = contrastingColor('#000', 'AA');

    expect(color1).toBe('rgb(151,151,151)');
    expect(color2).toBe('rgb(81,81,81)');
    expect(color3).toBe('rgb(255,249,255)');
    expect(color4).toBe('rgb(128,128,128)');
  });

  it('should accept other color strings', () => {
    const color1 = contrastingColor('rgb(0, 0, 0)');

    expect(color1).toBe('rgb(151,151,151)');
  });

  it('should accept transparent color strings', () => {
    const color1 = contrastingColor('rgba(0, 0, 0, 0.5)');

    expect(color1).toBe('rgba(151,151,151,0.675)');
  });
});
