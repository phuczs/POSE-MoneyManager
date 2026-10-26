---
name: android-compose-design
description: Create distinctive, production-grade Android UI in Jetpack Compose that escapes generic "AI default" aesthetics (one accent color on everything, Roboto, uniform rounded cards, flat symmetric grids). Use this skill whenever the user is building or restyling an Android app's interface in Kotlin/Compose — designing or redesigning screens, writing composables, setting up a Material 3 theme (color, typography, shape), building components, dashboards, lists, settings, or onboarding, or whenever they ask to make an Android app look better, more polished, more branded, or "less generic." Prefer this skill over web design skills (frontend-design, etc.) whenever the target is native Android — Compose, Material 3, Kotlin — even if the user does not say "Compose" explicitly.
---

# Android Compose Design

This skill guides the creation of distinctive, production-grade Android interfaces in Jetpack Compose that avoid generic "AI default" aesthetics. Produce real, compiling Kotlin/Compose code with a clear aesthetic point of view, plus a short rationale explaining the design choices.

The core stance: **build on top of Material 3, don't fight it.** Material 3 gives you accessibility, correct touch targets, dynamic color, and battle-tested components for free. Almost nothing that reads as "generic" is Material 3's fault — it's lazy, out-of-the-box *usage* of it (untouched defaults applied uniformly). The job is to keep Material 3's bones and engineering wins while escaping its default look.

## Start with a design direction

Before writing a single composable, commit to a clear visual voice. Generic output comes from having no point of view, so decide:

- **Who uses this and how?** A glanceable utility, a focused tool, a content app, a playful consumer app — each wants a different feel.
- **What is the ONE thing someone remembers?** A signature color move, a typographic personality, a distinctive layout, a motion moment. Pick something and make it land.
- **What is the tone?** Choose something with an edge rather than "clean and modern" (which is how everything ends up looking the same). Android-friendly directions: editorial/print-inspired, warm organic, precise technical/instrument-panel, soft neo-brutalist, luxe dark, energetic/playful, calm minimal, retro-digital. Use these for inspiration, then design one that's true to *this* app.

Intentionality beats intensity. Both bold maximalism and refined minimalism work — what fails is the timid, undecided middle. Translate the direction into concrete token decisions (palette, type, shape) before you build, and let it drive every choice.

## The generic-Compose tell (what you're escaping)

If the output has these traits, it's the framework default, not a design. Each one is the path of least resistance, which is exactly why it reads as "AI slop":

- **One accent color on everything.** A single hue (orange, purple, teal) on toggles, icons, FAB, progress, links — over a flat dark-slate surface. The accent stops meaning "important" because it's everywhere. The infamous default-purple baseline theme is the worst offender.
- **Untouched typography.** Roboto (the system default) and the stock Material type scale used verbatim. Titles and body sit too close in size/weight, so there's no hierarchy, and numbers wobble because they aren't tabular.
- **One corner radius on every surface.** Every card, chip, button, and container is the same rounded rectangle. Monotony reads as machine-generated.
- **A vertical stack of identical full-width cards.** Default `Card`s in a `Column` with uniform padding and one gap value, plus symmetric 2×2 grids of equal stat tiles. No focal point, no rhythm — every element shouts at the same volume.
- **Stock Material glyphs as the entire visual language.** A Material icon inside a colored circle, repeated, as the only "illustration." No custom iconography, brandmark, imagery, or texture.
- **The default `NavigationBar` selected-pill.** An immediate "stock Android" signal. So are raw, unrestyled M3 components everywhere.
- **Flat and static.** No elevation strategy, no motion, a dead solid background, no sense of light or depth.

## The escape toolkit

The highest-leverage moves, by dimension. Read the linked reference for the concrete Compose patterns and correct APIs before implementing.

**Color & theme** → `references/theming.md`
Commit to a dominant color story with *restrained, deliberate* accents — the accent earns attention by scarcity, so most surfaces stay neutral and the accent marks only what truly matters. Build real tonal depth: don't let `surface`, `surfaceContainer`, and `background` sit at one luminance, or everything floats on the same plane. Go beyond Material 3's default color roles with custom token sets when the direction needs them. Never paint large areas (a whole CTA bar, a hero card) in pure accent unless that *is* the concept.

**Typography** → `references/theming.md`
This is the single biggest lever against the generic look. Replace Roboto with a distinctive bundled or downloadable font; pair a characterful display family with a clean text family. Build a real hierarchy with dramatic size and weight jumps so headers, data, and body read as different ranks at a glance. Use tabular figures for any numbers (counts, stats, dashboards) so they align and don't jitter.

**Shape** → `references/theming.md`
Treat shape as identity, not an afterthought. Use *multiple* radii as a deliberate language (e.g., a large soft hero, crisp small chips), or commit fully to sharp/rectilinear for a technical voice — but don't default every container to the same rounded rectangle.

**Composition & hierarchy** → `references/composition.md`
Break the column-of-cards. Establish one focal point per screen (usually the most important data or action) and give it size, weight, color, or space the others don't get. Vary tile sizes to express importance instead of equal grids; use overlap, layering, and asymmetry for depth; alternate dense and airy sections for rhythm. Restyle or replace default `Card`s rather than shipping them raw.

**Depth & atmosphere** → `references/composition.md`
Escape the flat solid background. Add a sense of light with gradients, layered translucency, subtle blur/frosted panels, fine grain or texture, and a *deliberate* elevation/shadow system. Atmosphere is what separates a designed screen from a wireframe.

**Motion** → `references/motion.md`
One well-orchestrated moment beats ten twitchy micro-interactions. Invest in a staggered entrance on first load, animate state changes (selection, expand/collapse, counting numbers) instead of snapping, and consider shared-element transitions between screens. Use spring specs for natural motion — default linear easing feels robotic.

**Iconography & detail**
Go beyond stock glyphs-in-circles: a custom icon treatment, a brandmark, simple illustration, or at minimum intentional icon sizing and pairing. Sweat the details everyone skips — dividers, empty states, loading and error states, and the first-run experience.

## Stay bold *and* correct

Distinctiveness must never break usability, and this is where building on Material 3 pays off — keep its wins:

- Maintain readable contrast even with moody palettes (don't sacrifice legibility for atmosphere), keep touch targets at ~48dp, respect dynamic type scaling, and provide content descriptions.
- Support both light and dark themes unless the direction is deliberately single-mode.
- Use current, correct Compose and Material 3 APIs (`MaterialTheme`, `Scaffold`, `enableEdgeToEdge`, `WindowInsets`, `androidx.compose.material3`). Don't invent APIs or props. If you're unsure whether an API exists in a given version, say so rather than guess.

## What to produce

Deliver working code, not a description of code:

1. **A custom theme** — color tokens (light + dark), a `Typography` built on a real font, and `Shapes`, wired through `MaterialTheme`. Use a `CompositionLocal` for any extended tokens beyond Material 3's roles. See `references/theming.md`.
2. **The requested composable(s)**, consuming theme tokens (never hardcoded colors and sizes scattered inline), with hoisted state, `Modifier` parameters, and a `@Preview`.
3. **Idiomatic, compiling Kotlin** — correct imports, `@Composable`, Material 3 from `androidx.compose.material3`.
4. **A short design rationale** alongside the code: the direction chosen, the key token decisions (palette, type, shape) and why, and the two or three distinctive moves that make it not generic. Keep it tight — a few sentences, not an essay.

Deliver as real `.kt` files when the user wants something to drop into a project (a theme file plus screen files is a natural split); use inline code for small snippets. Whatever the form, pair it with the rationale.

## Reference files

- `references/theming.md` — Building a distinctive Compose theme: intentional `ColorScheme` and tonal depth, custom/extended color tokens via `CompositionLocal`, replacing Roboto with bundled or downloadable fonts, a real type hierarchy with tabular figures, and shape as a design language. **Read this first for almost any task** — the theme is where personality is won or lost.
- `references/composition.md` — Layout and hierarchy: focal points, breaking the uniform grid/stack, overlap and layering, restyling vs. rebuilding components, edge-to-edge and insets, spacing as a system, and atmosphere (gradients, translucency, blur, grain, elevation). Read when laying out screens.
- `references/motion.md` — Motion: entrance choreography, animating state with `animate*AsState` / `AnimatedContent` / `updateTransition`, animated counters, shared-element transitions, and spring-based feel. Read when adding animation.
