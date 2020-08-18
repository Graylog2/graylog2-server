Color utilities can be found at

```js static
import { util } from 'theme';
```

## colorLevel

Recreating [`color-level`](https://github.com/twbs/bootstrap/blob/08ba61e276a6393e8e2b97d56d2feb70a24fe22c/scss/_functions.scss#L97) from Bootstrap's SCSS functions

**`color`**
  - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")

**`level`**
  - any negative or positive number `-10` through `10`

Negative numbers render a lighter color, positive numbers get darker. Check out the follow example to see some samples of this in action.

```jsx harmony
import { ColorLevelExample } from './Utilities';

<ColorLevelExample />;
```

## contrastingColor
Accepts a color and [WCAG](https://www.w3.org/TR/WCAG21/#distinguishable) level, it then returns a properly contrasting color.

**`color`**
  - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")

**`wcagLevel`**
  - defaults: "AAA" -Based on the [contrast calculations recommended by W3](https://www.w3.org/WAI/WCAG21/Understanding/contrast-enhanced.html). (available levels: "AA", "AALarge", "AAA", "AAALarge")

```jsx harmony
import { ContrastingColorExample } from './Utilities';

<ContrastingColorExample />;
```

## readableColor
Generating a readable color following [W3C specs for readability](https://www.w3.org/TR/WCAG20-TECHS/G18.html).

**`color`**
  - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")

**`darkColor`**
  - defaults: Currently `color.global.textDefault`

**`lightColor`**
  - defaults: Currently `color.global.textAlt`

```jsx harmony
import { ReadableColorExample } from './Utilities';

<ReadableColorExample />;
```
