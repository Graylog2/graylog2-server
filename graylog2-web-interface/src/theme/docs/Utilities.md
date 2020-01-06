## colorLevel

Recreating [`color-level`](https://github.com/twbs/bootstrap/blob/08ba61e276a6393e8e2b97d56d2feb70a24fe22c/scss/_functions.scss#L97) from Bootstrap's SCSS functions

**`color`**
  - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")

**`level`**
  - any negative or positive number `-10` through `10`

Negative numbers render a lighter color, positive numbers get darker. Check out the follow example to see some samples of this in action.

```js
import { teinte, util } from 'theme';
import ColorSwatch from './Colors';

const { uno, quattro } = teinte.tertiary;

<>
  <p>
    <ColorSwatch name='uno -5' color={util.colorLevel(uno, -5)} />
    <ColorSwatch name='uno' color={uno} />
    <ColorSwatch name='uno +5' color={util.colorLevel(uno, 5)} />
  </p>
  <p>
    <ColorSwatch name='quattro -8' color={util.colorLevel(quattro, -8)} />
    <ColorSwatch name='quattro -2' color={util.colorLevel(quattro, -2)} />
    <ColorSwatch name='quattro' color={quattro} />
    <ColorSwatch name='quattro +2' color={util.colorLevel(quattro, 2)} />
    <ColorSwatch name='quattro +8' color={util.colorLevel(quattro, 8)} />
  </p>
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
  <p>
    <ColorSwatch name='uno AAA' color={util.contrastingColor(uno)} />
    <ColorSwatch name='uno' color={uno} />
    <ColorSwatch name='uno AA' color={util.contrastingColor(uno, 'AA')} />
  </p>
  <p>
    <ColorSwatch name='tre AAALarge' color={util.contrastingColor(tre, 'AAALarge')} />
    <ColorSwatch name='tre AAA' color={util.contrastingColor(tre)} />
    <ColorSwatch name='tre' color={tre} />
    <ColorSwatch name='tre AALarge' color={util.contrastingColor(tre, 'AALarge')} />
    <ColorSwatch name='tre AA' color={util.contrastingColor(tre, 'AA')} />
  </p>
  <p>
    <ColorSwatch name='quattro AAA' color={util.contrastingColor(quattro)} />
    <ColorSwatch name='quattro' color={quattro} />
    <ColorSwatch name='quattro AA' color={util.contrastingColor(quattro, 'AA')} />
  </p>
</>
```

## readableColor
Generating a readable color following W3C specs using [polished](https://polished.js.org/docs/#readablecolor)

**`color`**
  - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")

**`darkColor`**
  - defaults: Currently `teinte.primary.tre`

**`lightColor`**
  - defaults: Currently `teinte.primary.due`

```js
import { teinte, util } from 'theme';
import ColorSwatch from './Colors';

const { quattro, uno } = teinte.tertiary;
const { tre } = teinte.primary;

<>
  <p>
    <ColorSwatch name='uno' color={uno} />
    <ColorSwatch name='uno readableColor' color={util.readableColor(uno)} />
  </p>
  <p>
    <ColorSwatch name='tre' color={tre} />
    <ColorSwatch name='tre readableColor' color={util.readableColor(tre)} />
  </p>
  <p>
    <ColorSwatch name='quattro' color={quattro} />
    <ColorSwatch name='quattro readableColor' color={util.readableColor(quattro)} />
  </p>
</>
