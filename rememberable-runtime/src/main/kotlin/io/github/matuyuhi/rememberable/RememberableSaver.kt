package io.github.matuyuhi.rememberable

import android.os.Bundle
import android.os.Parcelable
import androidx.compose.runtime.saveable.Saver

/**
 * Creates a [Saver] for a [Parcelable] type that saves/restores via a [Bundle].
 */
inline fun <reified T : Parcelable> parcelableSaver(): Saver<T, Bundle> = Saver(
    save = { value ->
        Bundle().apply {
            putParcelable("value", value)
        }
    },
    restore = { bundle ->
        @Suppress("DEPRECATION")
        bundle.getParcelable("value")
    }
)
