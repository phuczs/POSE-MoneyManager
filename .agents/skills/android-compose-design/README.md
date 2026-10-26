# android-compose-design

A Claude [Agent Skill](https://docs.claude.com/en/docs/claude-code/skills) that helps Claude design **distinctive, production-grade Android UI in Jetpack Compose** — and escape the generic "AI default" look: one accent color on everything, Roboto, identical rounded cards, and flat symmetric grids.

The web already has skills like `frontend-design` for fighting generic AI aesthetics. This is the native-Android counterpart. It teaches Claude to build *on top of* Material 3 — keeping its accessibility and engineering wins — while breaking out of its default appearance through intentional theming, a real type hierarchy, deliberate shape and composition, motion, and atmosphere.

> **Status:** early. It works today and is genuinely useful, but it's still being refined — feedback and PRs welcome.

## The problem it solves

Ask an AI to build an Android screen and you usually get the framework defaults, applied uniformly:

- A single accent hue on toggles, icons, FAB, and progress, over a flat dark-slate surface
- Untouched Roboto and the stock Material type scale, so there's no real hierarchy
- One corner radius on every card, chip, and button
- A vertical stack of identical full-width cards and symmetric 2×2 stat grids with no focal point
- Stock Material glyphs-in-a-circle as the only visual language
- The default `NavigationBar` selected-pill, and no motion, depth, or atmosphere

It's clean, but it has no point of view. This skill gives Claude one.

## What's inside

```
android-compose-design/
├── SKILL.md                     # The engine: design process, the "generic tells", the escape toolkit, output contract
└── references/
    ├── theming.md               # Intentional ColorScheme & tonal depth, custom tokens, non-Roboto fonts, type hierarchy, shape
    ├── composition.md           # Focal points, breaking the grid, layering, restyling components, edge-to-edge, atmosphere
    └── motion.md                # Entrance choreography, animating state, counters, shared elements, spring feel
```

Claude reads `SKILL.md` when the task is relevant and pulls in the reference files only when it needs the detail (progressive disclosure), so they must stay together in the folder.

## Requirements

- A project using **Jetpack Compose** and **Material 3** (`androidx.compose.material3`), in Kotlin
- An agent that reads the `SKILL.md` format — **Claude Code** and **claude.ai** are the primary targets; the format is also read by Claude Desktop and other compatible agents

## Installation

### Claude Code

Drop the folder into a skills directory. **Project-scoped** (ships with the repo, available to teammates):

```bash
# from your Android project root
mkdir -p .claude/skills
git clone https://github.com/<your-username>/android-compose-design \
  .claude/skills/android-compose-design
```

**Personal** (available across all your projects):

```bash
git clone https://github.com/<your-username>/android-compose-design \
  ~/.claude/skills/android-compose-design
```

Then **restart Claude Code** — skills are discovered when the session starts — and list your available skills to confirm it loaded. Make sure `SKILL.md` sits directly inside `android-compose-design/`, not one folder deeper.

### claude.ai

Zip the `android-compose-design` folder and upload it under **Settings → Features → Skills**. (Custom skill upload is available on Pro, Max, Team, and Enterprise plans with code execution enabled.)

## Usage

Work **theme-first, then one screen at a time** — asking it to "fix the whole app" at once produces shallow, sweeping edits.

1. **Theme first.** Establish the foundation everything reuses:
   > Using the android-compose-design skill, redesign my Compose theme (Color / Type / Shape) in `ui/theme/` and explain the choices.
2. **Then each screen, pointed at the real file:**
   > Now redesign `ui/home/HomeScreen.kt` with the new theme.

   Review the diff, run it, give feedback, commit. Repeat per screen.

The skill loads automatically on UI/design requests, but naming it explicitly removes any doubt that it kicked in. Because Claude Code edits your real files, pointing it at actual paths means it rewrites your existing composables in place.

> **Note:** the skill will likely suggest a non-Roboto font. The agent can wire up `res/font/` or the downloadable-fonts dependency, but you'll need to supply and license the actual font.

## What it actually changes

The skill is opinionated about a handful of high-leverage dimensions:

- **Color** — a dominant story with *restrained* accents and real tonal depth, instead of one hue everywhere on a flat surface
- **Typography** — kill Roboto; pair a characterful display font with a clean text font; build a hierarchy with dramatic size/weight jumps; tabular figures for data
- **Shape** — treat corner radii as a deliberate language, not one rounded rectangle for everything
- **Composition** — a clear focal point, varied sizing, overlap and layering, rhythm — not a column of equal cards
- **Depth & atmosphere** — gradients, translucency, blur, grain, and a deliberate elevation system instead of a dead flat background
- **Motion** — one well-orchestrated moment (staggered entrance, animated state, counters, shared elements) over scattered micro-interactions

All while keeping Material 3's accessibility: readable contrast, ~48dp touch targets, dynamic type, and both light and dark themes.

## Roadmap

- Jetpack Compose (current)
- Possible future: XML/View-system support, Compose Multiplatform, and more ready-made design-direction recipes

## Contributing

Issues and pull requests are welcome — especially example before/after screens, additional component recipes, and refinements to the guidance. If you want to iterate on the skill itself, Anthropic's [skill-creator](https://docs.claude.com/en/docs/claude-code/skills) workflow (draft → test on real prompts → review → improve) is a good loop.

## Acknowledgements

Inspired by the spirit of Anthropic's `frontend-design` skill, rebuilt from scratch for native Android. This is an independent, original project and is **not affiliated with or endorsed by Anthropic**. Jetpack Compose and Material 3 are products of Google.

## License

[MIT](LICENSE) — do whatever you like, just keep the notice.
