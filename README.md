# DevPet

DevPet is a gamified IntelliJ Platform plugin that turns everyday coding into a virtual pet experience.

Your pet lives in a tool window, grows as you write code, and reacts to how you work:

- write code to earn XP and daily progress
- write tests and documentation for better rewards
- paste or AI-generate code and the pet gets sick
- complete builds to trigger fun pet reactions
- earn DevCoins and spend them in the shop on cosmetics

## Features

- **Virtual pet tool window** on the right side of the IDE
- **Code tracking** for lines written, tests, and Javadocs
- **AI detection** that checks batched code snippets with the OpenAI Chat Completions API
- **Build hook** that reacts to successful builds
- **Achievements and progression** for milestones and healthy coding habits
- **Cosmetics shop** for fun items like hats and backgrounds

## Requirements

- IntelliJ IDEA or another compatible IntelliJ-based IDE
- Java/Kotlin support enabled in the IDE
- An OpenAI API key if you want AI detection to run

## Configuration

AI detection reads the `DEVPET_OPENAI_API_KEY` environment variable.

You can also place the key in a `.env` file so the plugin can load it automatically.

Example:

```env
DEVPET_OPENAI_API_KEY=sk-...
```

If no key is available, DevPet still works — AI detection is simply disabled.

## Installation

### From a local build

1. Build the plugin.
2. Install the generated ZIP from disk in your IDE.

In IntelliJ IDEA:

<kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

### From source during development

Run the plugin inside a sandbox IDE:

```bash
./gradlew runIde
```

## Build

Create a distributable plugin archive with:

```bash
./gradlew buildPlugin
```

You can also verify the Kotlin sources compile with:

```bash
./gradlew compileKotlin
```

## Project structure

- `src/main/kotlin/com/github/alttrex/hackdelftchallenge/listeners/CodeTracker.kt` — document tracking and AI detection
- `src/main/kotlin/com/github/alttrex/hackdelftchallenge/listeners/BuildHook.kt` — build event reactions
- `src/main/kotlin/com/github/alttrex/hackdelftchallenge/toolWindow/` — DevPet UI components
- `src/main/kotlin/com/github/alttrex/hackdelftchallenge/state/` — pet state, progression, and health
- `src/main/resources/META-INF/plugin.xml` — plugin registration and metadata

## Presentation materials

Pitch deck source files live in:

- `PITCH.md`
- `slides/DevPet-Pitch.md`
- `slides/DevPet-Pitch.html`

## License

This project is based on the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template).
