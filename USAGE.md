# Usage

## Gradle Plugin

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("com.matuyuhi.rememberable") version "0.1.0"
}
```

That's it! The plugin will automatically add all necessary dependencies.

## Example

```kotlin
import android.os.Parcelable
import androidx.compose.runtime.saveable.rememberSaveable
import com.matuyuhi.rememberable.Rememberable
import kotlinx.parcelize.Parcelize

@Parcelize
@Rememberable
data class UserState(
    val name: String,
    val age: Int,
) : Parcelable

@Composable
fun UserScreen() {
    val userState = rememberSaveable { UserState("John", 30) }
    // ...
}
```

## Dependencies

The plugin automatically adds the following dependencies:

- `com.matuyuhi:rememberable-annotations:0.1.0`
- `com.matuyuhi:rememberable-runtime:0.1.0`

You don't need to add them manually!
