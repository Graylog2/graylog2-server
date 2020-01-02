## colorLevel

Recreating [`color-level`](https://github.com/twbs/bootstrap/blob/08ba61e276a6393e8e2b97d56d2feb70a24fe22c/scss/_functions.scss#L97) from Bootstrap's SCSS functions

Negative numbers render a lighter color, positive numbers get darker. Check out the follow example to see some samples of this in action.

```js
import { teinte, util } from 'theme';
import ColorSwatch from './Colors';

const { uno, quattro } = teinte.tertiary;

<>
  <ColorSwatch name='uno -5' color={util.colorLevel(uno, -5)} copyText='' />
  <ColorSwatch name='uno' color={uno} copyText='' />
  <ColorSwatch name='uno +5' color={util.colorLevel(uno, 5)} copyText='' />

  <ColorSwatch name='quattro -8' color={util.colorLevel(quattro, -8)} copyText='' />
  <ColorSwatch name='quattro -2' color={util.colorLevel(quattro, -2)} copyText='' />
  <ColorSwatch name='quattro' color={quattro} copyText='' />
  <ColorSwatch name='quattro +2' color={util.colorLevel(quattro, 2)} copyText='' />
  <ColorSwatch name='quattro +8' color={util.colorLevel(quattro, 8)} copyText='' />
</>
```

## contrastingColor
Accepts a color and [WCAG](https://www.w3.org/TR/WCAG21/#distinguishable) level, it then returns a properly contrasting color.

**`color`**
  - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")

**`wcagLevel`**
  - defaults: "AAA" -Based on the [contrast calculations recommended by W3](https://www.w3.org/WAI/WCAG21/Understanding/contrast-enhanced.html). (available levels: "AA", "AALarge", "AAA", "AAALarge")

```js
import { teinte, util } from 'theme';
import ColorSwatch from './Colors';

const { quattro, uno } = teinte.tertiary;
const { tre } = teinte.primary;

<>
  <ColorSwatch name='uno AAA' color={util.contrastingColor(uno)} copyText='' />
  <ColorSwatch name='uno' color={uno} copyText='' />
  <ColorSwatch name='uno AA' color={util.contrastingColor(uno, 'AA')} copyText='' />

  <ColorSwatch name='tre AAALarge' color={util.contrastingColor(tre, 'AAALarge')} copyText='' />
  <ColorSwatch name='tre AAA' color={util.contrastingColor(tre)} copyText='' />
  <ColorSwatch name='tre' color={tre} copyText='' />
  <ColorSwatch name='tre AALarge' color={util.contrastingColor(tre, 'AALarge')} copyText='' />
  <ColorSwatch name='tre AA' color={util.contrastingColor(tre, 'AA')} copyText='' />

  <ColorSwatch name='quattro AAA' color={util.contrastingColor(quattro)} copyText='' />
  <ColorSwatch name='quattro' color={quattro} copyText='' />
  <ColorSwatch name='quattro AA' color={util.contrastingColor(quattro, 'AA')} copyText='' />
</>
```

## readableColor
