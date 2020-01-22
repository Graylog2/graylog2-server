## colorLevel

Recreating [`color-level`](https://github.com/twbs/bootstrap/blob/08ba61e276a6393e8e2b97d56d2feb70a24fe22c/scss/_functions.scss#L97) from Bootstrap's SCSS functions

**`color`**
  - any string that represents a color (ex: `#f00` or `rgb(255, 0, 0)`)

**`level`**
  - any negative or positive number `-10` through `10`

Negative numbers render a lighter color, positive numbers get darker. Check out the follow example to see some samples of this in action.

```js
import { color, util } from 'theme';
import ColorSwatch from './Colors';

console.log('colors', color.teinte);

const { info, primary } = color.teinte.variant;

<>
  <p>
    <ColorSwatch name='info -5' color={util.colorLevel(info, -5)} />
    <ColorSwatch name='info' color={info} />
    <ColorSwatch name='info +5' color={util.colorLevel(info, 5)} />
  </p>
  <p>
    <ColorSwatch name='primary -8' color={util.colorLevel(primary, -8)} />
    <ColorSwatch name='primary -2' color={util.colorLevel(primary, -2)} />
    <ColorSwatch name='primary' color={primary} />
    <ColorSwatch name='primary +2' color={util.colorLevel(primary, 2)} />
    <ColorSwatch name='primary +8' color={util.colorLevel(primary, 8)} />
  </p>
</>
```

## contrastingColor
Accepts a color and [WCAG distinguishable level](https://www.w3.org/TR/WCAG21/#distinguishable), it then returns a properly contrasting color.

**`color`**
  - any string that represents a color (ex: `#f00` or `rgb(255, 0, 0)`)

**`wcagLevel`**
  - defaults: "AAA" -Based on the [contrast calculations recommended by W3](https://www.w3.org/WAI/WCAG21/Understanding/contrast-enhanced.html). (available levels: "AA", "AALarge", "AAA", "AAALarge")

```js
import { color, util } from 'theme';
import ColorSwatch from './Colors';

const { primary, info } = color.teinte.variant;
const { textDefault } = color.teinte.global;

<>
  <p>
    <ColorSwatch name='info AAA' color={util.contrastingColor(info)} />
    <ColorSwatch name='info' color={info} />
    <ColorSwatch name='info AA' color={util.contrastingColor(info, 'AA')} />
  </p>
  <p>
    <ColorSwatch name='textDefault AAALarge' color={util.contrastingColor(textDefault, 'AAALarge')} />
    <ColorSwatch name='textDefault AAA' color={util.contrastingColor(textDefault)} />
    <ColorSwatch name='textDefault' color={textDefault} />
    <ColorSwatch name='textDefault AALarge' color={util.contrastingColor(textDefault, 'AALarge')} />
    <ColorSwatch name='textDefault AA' color={util.contrastingColor(textDefault, 'AA')} />
  </p>
  <p>
    <ColorSwatch name='primary AAA' color={util.contrastingColor(primary)} />
    <ColorSwatch name='primary' color={primary} />
    <ColorSwatch name='primary AA' color={util.contrastingColor(primary, 'AA')} />
  </p>
</>
```

## readableColor
Returns `textDefault` or `textAlt` (or optional light and dark return colors) for best contrast depending on the luminosity of the given color. Follows [W3C specs for readability](https://www.w3.org/TR/WCAG20-TECHS/G18.html).

**`color`**
  - any string that represents a color (ex: `#f00` or `rgb(255, 0, 0)`)

**`darkColor`**
  - defaults: Currently `teinte.global.textDefault`

**`lightColor`**
  - defaults: Currently `teinte.global.textAlt`

```js
import { color, util } from 'theme';
import ColorSwatch from './Colors';

const { primary, info, dark, light } = color.teinte.variant;
const { textDefault, textAlt } = color.teinte.global;

<>
  <p>
    <ColorSwatch name='info readableColor' color={util.readableColor(info)} />
  </p>
  <p>
    <ColorSwatch name='textAlt readableColor' color={util.readableColor(textAlt)} />
  </p>
  <p>
    <ColorSwatch name='primary readableColor w/ options'
                 color={util.readableColor(primary, dark.primary, light.primary)} />
  </p>
</>
