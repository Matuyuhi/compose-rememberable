package androidx.compose.runtime.saveable

interface SaverScope {
    fun canBeSaved(value: Any): Boolean
}

interface Saver<Original, Saveable : Any> {
    fun SaverScope.save(value: Original): Saveable?
    fun restore(value: Saveable): Original?
}

fun <Original, Saveable : Any> Saver(
    save: SaverScope.(value: Original) -> Saveable?,
    restore: (value: Saveable) -> Original?,
): Saver<Original, Saveable> {
    return object : Saver<Original, Saveable> {
        override fun SaverScope.save(value: Original): Saveable? = save(value)
        override fun restore(value: Saveable): Original? = restore(value)
    }
}
