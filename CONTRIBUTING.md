# Contributing to Cardinal Maps

Thank you for your interest in contributing to Cardinal Maps! We would love your help. Here are some tips and guidelines for how to get started. If your quesiton isn't answered here please feel free to [open an issue](https://github.com/ellenhp/cardinal/issues/new).

## Code of Conduct

First things first, the Cardinal project adheres to the [Rust Code of Conduct](https://www.rust-lang.org/policies/code-of-conduct). You are expected to uphold these values while participating. Please report unacceptable behavior to [the lead maintainer](mailto:ellen.h.poe@gmail.com).

## How to Contribute

### Reporting Issues

- Use the GitHub issue tracker to report bugs or suggest features
- Before creating a new issue, try to search existing issues to avoid duplicates
- Include as much relevant information as possible in your report:
    - Steps to reproduce the issue
    - Expected vs actual behavior
    - Screenshots if applicable
    - Device and OS information for Android issues

### Pull Request Process

1. Fork the repository and create your branch from `main`
2. Do your best to add some tests (at press time this is mostly aspirational)
3. Ensure your test suite passes
4. Make sure your code follows the existing style for whichever component(s) you're modifying
5. Update documentation as needed
6. Submit your pull request with a clear description of your changes

## Development Guidelines

### Android App

The Android app is built with Kotlin and follows modern Android development practices.

#### Code Style
- Use meaningful variable and function names
- Keep functions small and focused when you can
- Prefer immutable data structures where possible

#### Architecture
- Follow MVVM (Model-View-ViewModel) architecture pattern
- Use dependency injection with Hilt
- Separate UI logic from business logic

#### Design Principles
- Follow [Material 3 guidelines](https://m3.material.io/) for components, typography, and color schemes
- Maintain consistency in UI patterns throughout the app
- Prioritize accessibility and usability for all users
- Aim for a clean, minimalist aesthetic

#### Implementation
- Use Android Compose for all UI components
- Use Material 3 components and themes where possible
- Try to implement adaptive layouts that work across different screen sizes (currently aspirational)
- Follow the existing color scheme and typography defined in the theme

#### Building
```bash
cd cardinal-android
./gradlew assembleDebug
```

### Rust components

The offline geocoder is written in Rust and changes to it should follow Rust best practices.

#### Code Style
- Use `rustfmt` for code formatting
- Use `clippy` for linting
- Write idiomatic Rust code whenever possible
    - We use UniFFI to expose rust code to Kotlin (and in the future, Swift) which imposes some limits

### Privacy and Security
- Respect user privacy in all code changes
- Avoid mistakes that could leak user data to online services without knowledge or consent (consent entails knowledge)

## Getting Help

If you need help with your contribution, feel free to:
- Ask questions in the PR comments
- Open an issue for discussion about larger changes
- Contact the maintainers directly

## Generative AI Policy

Unlike quite a few projects, we do accept contributions created with generative AI tooling with some very important caveats:

1. You must fully review all AI-generated code before submission (this should be obvious, and yet...)
2. Contributors are themselves responsible for the quality and correctness of all code, including AI-generated code they submit
3. Core business logic should not be "vibe-coded"—you may use AI for boilerplate, code review or idea generation, but not for making intricate changes to core functionality that you don't fully understand

Please see our full [Generative AI Policy](GEN_AI_POLICY.md) for more details. Do not abuse this policy.

