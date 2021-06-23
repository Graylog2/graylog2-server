Color utilities can be found at

```js static
import { useTheme } from 'styled-components';
const { utils } = useTheme();
```

## colorLevel

Recreating [`color-level`](https://github.com/twbs/bootstrap/blob/08ba61e276a6393e8e2b97d56d2feb70a24fe22c/scss/_functions.scss#L97) from Bootstrap's SCSS functions

**`color`**
  - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")

**`level`**
  - any negative or positive number `-10` through `10`

Negative numbers render a lighter color, positive numbers get darker. Check out the follow example to see some samples of this in action.

```jsx
import { useTheme } from 'styled-components';
import ColorSwatch from './Colors';

const { colors, utils } = useTheme();

const { info, primary } = colors.variant;

<>
  <p>
    <ColorSwatch name='info -5' color={utils.colorLevel(info, -5)} />
    <ColorSwatch name='info' color={info} />
    <ColorSwatch name='info +5' color={utils.colorLevel(info, 5)} />
  </p>
  <p>
    <ColorSwatch name='primary -8' color={utils.colorLevel(primary, -8)} />
    <ColorSwatch name='primary -2' color={utils.colorLevel(primary, -2)} />
    <ColorSwatch name='primary' color={primary} />
    <ColorSwatch name='primary +2' color={utils.colorLevel(primary, 2)} />
    <ColorSwatch name='primary +8' color={utils.colorLevel(primary, 8)} />
  </p>
</>
```

## contrastingColor
Accepts a color and [WCAG](https://www.w3.org/TR/WCAG21/#distinguishable) level, it then returns a properly contrasting color.

**`color`**
  - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")

**`wcagLevel`**
  - defaults: "AAA" -Based on the [contrast calculations recommended by W3](https://www.w3.org/WAI/WCAG21/Understanding/contrast-enhanced.html). (available levels: "AA", "AALarge", "AAA", "AAALarge")

```jsx
import { useTheme } from 'styled-components';
import ColorSwatch from './Colors';

const { colors, utils } = useTheme();

const { info, primary } = colors.variant;
const { textDefault } = colors.global;

<>
  <p>
    <ColorSwatch name='info AAA' color={utils.contrastingColor(info)} />
    <ColorSwatch name='info' color={info} />
    <ColorSwatch name='info AA' color={utils.contrastingColor(info, 'AA')} />
  </p>
  <p>
    <ColorSwatch name='textDefault AAALarge' color={utils.contrastingColor(textDefault, 'AAALarge')} />
    <ColorSwatch name='textDefault AAA' color={utils.contrastingColor(textDefault)} />
    <ColorSwatch name='textDefault' color={textDefault} />
    <ColorSwatch name='textDefault AALarge' color={utils.contrastingColor(textDefault, 'AALarge')} />
    <ColorSwatch name='textDefault AA' color={utils.contrastingColor(textDefault, 'AA')} />
  </p>
  <p>
    <ColorSwatch name='primary AAA' color={utils.contrastingColor(primary)} />
    <ColorSwatch name='primary' color={primary} />
    <ColorSwatch name='primary AA' color={utils.contrastingColor(primary, 'AA')} />
  </p>
</>
```

## readableColor
Generating a readable color following [W3C specs for readability](https://www.w3.org/TR/WCAG20-TECHS/G18.html).

**`color`**
  - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")

**`darkColor`**
  - defaults: Currently `color.global.textDefault`

**`lightColor`**
  - defaults: Currently `color.global.textAlt`

```jsx
import { useTheme } from 'styled-components';
import ColorSwatch from './Colors';

const { colors, utils } = useTheme();

const { info, primary } = colors.variant;
const { textDefault } = colors.global;

<>
  <p>
    <ColorSwatch name='info' color={info} />
    <ColorSwatch name='info readableColor' color={utils.readableColor(info)} />
  </p>
  <p>
    <ColorSwatch name='textDefault' color={textDefault} />
    <ColorSwatch name='textDefault readableColor' color={utils.readableColor(textDefault)} />
  </p>
  <p>
    <ColorSwatch name='primary' color={primary} />
    <ColorSwatch name='primary readableColor' color={utils.readableColor(primary)} />
  </p>
</>
```
