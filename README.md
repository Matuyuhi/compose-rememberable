# compose-rememberable

Automatically generate `rememberSaveable` for Compose state.

## Features

- 🎯 Zero boilerplate - just add `@Rememberable` annotation
- 🚀 No `Parcelable` required - works with any data class
- 📦 Automatic `Saver` generation for Compose
- 🔌 Easy setup with Gradle plugin

## Usage

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("com.matuyuhi.rememberable") version "0.1.0"
}
```

That's it! The plugin automatically adds all necessary dependencies.

## Example

```kotlin
import androidx.compose.runtime.rememberSaveable
import com.matuyuhi.rememberable.Rememberable

@Rememberable
data class UserState(
    val name: String,
    val age: Int,
)

@Composable
fun UserScreen() {
    val userState by rememberSaveable { UserState("John", 30) }
    // ...
}
```

## How It Works

The compiler plugin automatically generates a `Saver` property in the companion object of classes annotated with `@Rememberable`:

```kotlin
@Rememberable
data class UserState(...)

// The compiler generates:
companion object {
    val Saver: Saver<UserState, Bundle>
}
```

The `Saver` saves/restores each property via `Bundle.putString()` and `Bundle.getString()`.

## Requirements

- Android SDK 24+
- Kotlin 2.2.10+
- Compose BOM 2024.09.00+

## License

Apache License 2.0
