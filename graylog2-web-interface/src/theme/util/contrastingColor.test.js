import contrastingColor from './contrastingColor';

describe('contrastingColor', () => {
  it('should return a properly contrasting color', () => {
    const color1 = contrastingColor('#000');
    const color2 = contrastingColor('#fff');
    const color3 = contrastingColor('#f0f');
    const color4 = contrastingColor('#000', 'AA');

    expect(color1).toBe('#999');
    expect(color2).toBe('#595959');
    expect(color3).toBe('#fff2ff');
    expect(color4).toBe('#7f7f7f');
  });

  it('should accept other color strings', () => {
    const color1 = contrastingColor('rgb(0, 0, 0)');

    expect(color1).toBe('#999');
  });

  it('should accept transparent color strings', () => {
    const color1 = contrastingColor('rgba(0, 0, 0, 0.5)');

    expect(color1).toBe('rgba(157,157,157,0.825)');
  });
});
