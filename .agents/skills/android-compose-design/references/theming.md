# Theming: where personality is won or lost

The theme is the highest-leverage place to escape the generic look. A distinctive `ColorScheme`, a real font, a deliberate type hierarchy, and an intentional shape language do more than any single screen layout. Build a real token system and route everything through it — never scatter hardcoded `Color(0xFF...)` and `12.dp` values across composables.

## Color

### Define an intentional ColorScheme, not the generated baseline

The default Material baseline (or a single hue dropped into the theme wizard) is the source of the "one accent on slate" look. Hand-pick values for both themes:

```kotlin
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB877),        // accent — used sparingly, for emphasis only
    onPrimary = Color(0xFF3A1D00),
    primaryContainer = Color(0xFF5A3210),
    secondary = Color(0xFF8FA68E),      // a second, quieter voice
    background = Color(0xFF0E1011),     // near-black, slightly warm
    surface = Color(0xFF15181A),        // base card
    surfaceContainer = Color(0xFF1C2023),    // raised — note the luminance step
    surfaceContainerHigh = Color(0xFF24292D), // higher still
    onSurface = Color(0xFFE6E3DE),
    onSurfaceVariant = Color(0xFF9AA0A0),    // muted secondary text
    outline = Color(0xFF3A4042),
)
```

Two principles matter more than the exact hex values:

- **Tonal depth.** `background`, `surface`, and the `surfaceContainer*` roles must sit at *distinct* luminances. When they're all within a few percent, the UI is flat and everything floats on one plane (the NotifIQ tell). Step them deliberately so raised elements read as raised.
- **Accent restraint.** `primary` is for emphasis and primary actions only. If it's on toggles, icons, progress, *and* buttons, it stops signaling importance. Keep most of the screen neutral so the accent earns its presence.

### Extended tokens beyond Material 3's roles

Material's roles don't cover everything (brand gradients, success/warning, a custom highlight). Add your own via a `CompositionLocal` so they theme correctly:

```kotlin
@Immutable
data class ExtendedColors(
    val success: Color,
    val warning: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified)
}
```

Provide them in the theme and read with `LocalExtendedColors.current` (wired below).

### Dynamic color is a choice, not a default

You *can* support Material You dynamic color on Android 12+:

```kotlin
val colors = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    dark -> DarkColors
    else -> LightColors
}
```

But a branded app usually wants a *fixed* identity. Don't reach for dynamic color just because it's available — decide whether the app's personality should bend to the user's wallpaper or stay constant.

## Typography — the biggest single lever

Roboto is why so many apps look identical. Replace it.

### Bundled fonts

Drop `.ttf`/`.otf` files in `res/font/`, then build families:

```kotlin
val Display = FontFamily(
    Font(R.font.clash_display_medium, FontWeight.Medium),
    Font(R.font.clash_display_semibold, FontWeight.SemiBold),
)
val Text = FontFamily(
    Font(R.font.inter_tight_regular, FontWeight.Normal),
    Font(R.font.inter_tight_medium, FontWeight.Medium),
)
```

### Downloadable fonts (when you can't bundle)

Use the Google Fonts provider — add `androidx.compose.ui:ui-text-google-fonts`, declare the provider (with certs) in the manifest, then:

```kotlin
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)
val Display = FontFamily(Font(GoogleFont("Fraunces"), provider, FontWeight.SemiBold))
```

Downloadable fonts resolve asynchronously, so set a sensible fallback. Bundle fonts when you need the text correct on first frame.

### Build a real hierarchy

Pair a characterful display family with a clean text family, and make the ranks *obviously* different — timid 2sp steps are why default type reads flat:

```kotlin
val AppType = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontSize = 40.sp, lineHeight = 44.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    titleMedium = TextStyle(fontFamily = Text, fontSize = 16.sp, lineHeight = 22.sp,
        fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontFamily = Text, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = Text, fontSize = 11.sp, letterSpacing = 0.8.sp), // for tags/eyebrows
)
```

### Tabular figures for data

Any number that changes or aligns in a column (counts, stats, dashboards, the NotifIQ tiles) should use tabular figures so digits don't jitter:

```kotlin
val StatNumber = TextStyle(
    fontFamily = Display, fontSize = 28.sp, fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = "tnum",   // tabular numerals
)
```

## Shape as a design language

Shape is an underused identity lever. Define `Shapes` with intentional, *varied* corners — or commit fully to sharp for a technical/instrument feel:

```kotlin
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(24.dp),     // soft hero containers
    extraLarge = RoundedCornerShape(32.dp),
)
```

Then apply shapes *by role* (crisp chips, a soft hero card) rather than one radius everywhere. For an expressive direction, asymmetric or cut-corner shapes via `RoundedCornerShape(topStart = ..., bottomEnd = ...)` or a custom `Shape` add real character.

## Wiring it together

```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extended = if (darkTheme) DarkExtended else LightExtended

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppType,
            shapes = AppShapes,
            content = content,
        )
    }
}

// Convenience accessor mirroring MaterialTheme.colorScheme
object AppTheme {
    val extendedColors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
```

Composables then read `MaterialTheme.colorScheme.primary`, `MaterialTheme.typography.displayLarge`, `MaterialTheme.shapes.large`, and `AppTheme.extendedColors.success` — consistent, themeable, and easy to restyle later.

## Accessibility check

A moody palette still has to be legible. Keep body text and surfaces at WCAG-passing contrast, verify both light and dark, and don't rely on the accent color alone to convey state (pair it with a label or icon). Boldness and legibility are not a trade-off if you choose values deliberately.
