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

const { info, primary } = teinte.variant;

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
Accepts a color and [WCAG](https://www.w3.org/TR/WCAG21/#distinguishable) level, it then returns a properly contrasting color.

**`color`**
  - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")

**`wcagLevel`**
  - defaults: "AAA" -Based on the [contrast calculations recommended by W3](https://www.w3.org/WAI/WCAG21/Understanding/contrast-enhanced.html). (available levels: "AA", "AALarge", "AAA", "AAALarge")

```js
import { teinte, util } from 'theme';
import ColorSwatch from './Colors';

const { info, primary } = teinte.variant;
const { textDefault } = teinte.global;

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
Generating a readable color following W3C specs using [polished](https://polished.js.org/docs/#readablecolor)

**`color`**
  - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")

**`darkColor`**
  - defaults: Currently `teinte.global.textDefault`

**`lightColor`**
  - defaults: Currently `teinte.global.textAlt`

```js
import { teinte, util } from 'theme';
import ColorSwatch from './Colors';

const { info, primary } = teinte.variant;
const { textDefault } = teinte.global;

<>
  <p>
    <ColorSwatch name='info' color={info} />
    <ColorSwatch name='info readableColor' color={util.readableColor(info)} />
  </p>
  <p>
    <ColorSwatch name='textDefault' color={textDefault} />
    <ColorSwatch name='textDefault readableColor' color={util.readableColor(textDefault)} />
  </p>
  <p>
    <ColorSwatch name='primary' color={primary} />
    <ColorSwatch name='primary readableColor' color={util.readableColor(primary)} />
  </p>
</>
