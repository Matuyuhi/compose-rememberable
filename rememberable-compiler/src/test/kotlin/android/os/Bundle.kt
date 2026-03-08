package android.os

open class Bundle {
    private val map = mutableMapOf<String, Any?>()

    fun putString(key: String, value: String?) {
        map[key] = value
    }

    fun getString(key: String, defaultValue: String): String {
        return map[key] as? String ?: defaultValue
    }

    fun putParcelable(key: String, value: Parcelable?) {
        map[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Parcelable> getParcelable(key: String): T? {
        return map[key] as? T
    }
}
