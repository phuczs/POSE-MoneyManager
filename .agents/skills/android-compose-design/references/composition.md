# Composition, hierarchy & atmosphere

The "vertical `Column` of identical full-width `Card`s" is the generic skeleton. Break it with intentional hierarchy, spatial variety, and a sense of light.

## Establish a focal point

Every screen has ONE thing the eye should hit first — usually the most important number or action. Give it size, weight, color, or space the others don't get. The NotifIQ home screen fails here: four equal stat tiles in a 2×2 grid all compete at the same volume, so nothing leads. Pick the hero (e.g., the noise-reduction figure, rendered large) and let the rest support it.

## Break the uniform grid and stack

- **Vary size to express importance.** Instead of four equal tiles, use one featured large card plus smaller secondary ones, or a horizontal scroller of secondary stats under a dominant primary metric. Equal-weight grids read as "I had four numbers and no opinion about them."
- **Layer and overlap for depth.** `Box` with `Modifier.offset`, `zIndex`, and negative padding lets elements break out of their containers — a large number overlapping a header band, a card peeking past a section edge. This single move kills the flat stacked-cards feel.

```kotlin
Box {
    HeaderBand(Modifier.fillMaxWidth().height(140.dp))
    StatCard(
        Modifier
            .align(Alignment.BottomStart)
            .offset(x = 20.dp, y = 40.dp)   // overlaps the band below it
            .zIndex(1f)
    )
}
```

- **Rhythm, not uniform gaps.** Alternate dense and airy sections rather than applying one 16.dp gap everywhere. Define a spacing scale and use it deliberately (see "Spacing as a system").

## Restyle or rebuild components — don't ship them raw

Raw `Card { }` / `ElevatedCard` with default radius and elevation everywhere is a stock signal. Two approaches, in order of preference:

1. **Restyle the M3 component** via its parameters — `colors =`, `shape =`, `border =`, `contentPadding =`, `tonalElevation =`. This keeps the component's behavior and accessibility while changing the look. Reach for this first.
2. **Build a custom container** (`Surface` or `Box` with your shape, colors, border, and elevation) only when the direction demands something M3 can't express.

A distinctive stat tile, for example, isn't a flat rounded box with a centered number — it might be left-aligned with a small eyebrow label, an oversized tabular figure, a thin accent rule, and a subtle gradient or border:

```kotlin
@Composable
fun StatTile(label: String, value: Int, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    ) {
        Column(Modifier.padding(start = 18.dp, top = 16.dp, bottom = 16.dp, end = 16.dp)) {
            Box(Modifier.size(width = 24.dp, height = 3.dp).background(accent, RoundedCornerShape(2.dp)))
            Spacer(Modifier.height(14.dp))
            Text(value.toString(), style = StatNumber)               // tabular figures
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

**Bottom navigation** deserves special attention: the default `NavigationBar` selected-pill is one of the strongest "stock Android" tells. At minimum restyle the indicator and colors; better, build a custom bar whose selected state fits your design language (an underline, a dot, a morphing shape, a color/scale shift).

## Edge-to-edge and insets

Modern Android is edge-to-edge; handling it well is a big part of looking polished. Call `enableEdgeToEdge()` in the Activity (on API 35+ it's the default), and let backgrounds and atmosphere run *under* the system bars while content respects them:

```kotlin
Scaffold { innerPadding ->
    Box(
        Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)   // runs edge to edge, under the bars
    ) {
        Column(Modifier.padding(innerPadding)) { /* content respects bars */ }
    }
}
```

Use `Modifier.windowInsetsPadding(...)`, `.systemBarsPadding()`, or `.safeDrawingPadding()` for finer control. Avoid the "content jammed under a default `TopAppBar`" look — consider a custom collapsing header or generous top space instead.

## Atmosphere & depth — escape the flat background

A dead solid fill is a wireframe. Add a sense of light:

- **Gradients.** `Brush.linearGradient` / `verticalGradient` / `radialGradient` as a background or accent. Even a subtle two-stop gradient on the background reads as lit rather than printed. Radial glows behind a hero element add focus.
- **Layered translucency.** Semi-transparent surfaces over a gradient create depth without heavy elevation.
- **Blur / frosted panels.** `Modifier.blur(...)` for soft glows or frosted overlays (API 31+ via `RenderEffect`; provide a non-blurred fallback below 31). Great for a translucent bottom bar or a glow behind a focal number.
- **Texture / grain.** A subtle tiled noise drawable at low alpha, or a faint pattern, kills the "plastic flat" feel and adds craft.
- **Deliberate elevation.** Treat shadows/`tonalElevation` as a *system* with a consistent light source and clear tiers — not uniform everywhere and not absent. `Modifier.shadow(elevation, shape, spotColor = ...)` lets you tint shadows to match the palette.

```kotlin
val backgroundGradient = Brush.verticalGradient(
    listOf(
        MaterialTheme.colorScheme.background,
        AppTheme.extendedColors.gradientEnd,   // a slightly different tone at the bottom
    )
)
```

## Spacing as a system

Define a scale (e.g., 4 / 8 / 12 / 16 / 24 / 32 / 48 dp) and apply it deliberately for rhythm. Expose it as tokens if it helps consistency. Generous, *intentional* negative space around the focal point — and tighter grouping of related items — does as much for perceived quality as any color choice.
