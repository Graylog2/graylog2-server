import contrastingColor from './contrastingColor';

describe('contrastingColor', () => {
  it('should return a properly contrasting color', () => {
    const color1 = contrastingColor('#000');
    const color2 = contrastingColor('#fff');
    const color3 = contrastingColor('#f0f');
    const color4 = contrastingColor('#000', 'AAA');

    expect(color1).toBe('#808080');
    expect(color2).toBe('#737373');
    expect(color3).toBe('#4d004d');
    expect(color4).toBe('#999');
  });

  it('should accept other color strings', () => {
    const color1 = contrastingColor('rgb(0, 0, 0)');

    expect(color1).toBe('#808080');
  });
});
