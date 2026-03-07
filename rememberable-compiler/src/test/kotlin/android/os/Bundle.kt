package android.os

open class Bundle {
    private val map = mutableMapOf<String, Any?>()

    fun putParcelable(key: String, value: Parcelable?) {
        map[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Parcelable> getParcelable(key: String): T? {
        return map[key] as? T
    }
}
