# Motion

Motion is the difference between "a screen" and "a product." But scattered micro-interactions feel noisy and cheap — invest in one well-orchestrated moment rather than animating everything that moves.

## Entrance choreography

A staggered reveal of a screen's key elements on first composition creates more delight than any number of hover-equivalent effects. Drive it with per-item delay:

```kotlin
val items = listOf(/* ... */)
items.forEachIndexed { index, item ->
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 60L)   // staggered cascade
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 4 },
    ) { Row(item) }
}
```

## Animate state — don't snap

When something changes, transition to the new value instead of jumping:

- **`animate*AsState`** for color, size, offset, alpha on selection, expand/collapse, or theme toggle:

```kotlin
val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
val scale by animateFloatAsState(if (pressed) 0.97f else 1f)
```

- **`AnimatedContent`** for swapping content — a changing stat value, a tab switch — with a tasteful transition.
- **`updateTransition`** to coordinate several properties of one component's state together (color + size + corner radius shifting as one).
- **Animated counters** for dashboards (the NotifIQ tiles counting up reads far better than appearing fully formed):

```kotlin
val shown by animateIntAsState(targetValue = count, animationSpec = tween(700))
Text(shown.toString(), style = StatNumber)
```

## Navigation & shared elements

For continuity between a list and its detail, use the shared-element transition API (`SharedTransitionLayout`, `Modifier.sharedElement(rememberSharedContentState(key), ...)`) so an item appears to grow into the detail screen rather than cross-fading. This is in `androidx.compose.animation` and is opt-in / version-gated — confirm it's available in the project's Compose version before relying on it. For destination changes generally, `AnimatedContent` or the Navigation library's transition specs give intentional enter/exit motion instead of the default.

## Feel: prefer springs

Default linear or no-easing motion feels robotic. Reach for springs for natural, physical movement, and reserve `tween` (with a real easing curve like `FastOutSlowInEasing`) for precise, timed reveals:

```kotlin
val offset by animateDpAsState(
    targetValue = target,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
)
```

Press feedback, swipe gestures, and pull-to-refresh all benefit from spring physics.

## Restraint and performance

Don't animate everything — a few intentional moments read as crafted; constant motion reads as busy. Keep transitions subtle, consider users who prefer reduced motion, and avoid jank: keep animations cheap and don't trigger heavy recomposition on every frame (use `derivedStateOf` / `graphicsLayer` lambdas where it helps). One memorable moment beats ten twitchy ones.
