package android.os

open class Bundle {
    private val map = mutableMapOf<String, Any?>()

    fun putString(key: String, value: String?) {
        map[key] = value
    }

    fun getString(key: String, defaultValue: String): String {
        return map[key] as? String ?: defaultValue
    }

    fun putInt(key: String, value: Int) {
        map[key] = value
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return map[key] as? Int ?: defaultValue
    }

    fun putLong(key: String, value: Long) {
        map[key] = value
    }

    fun getLong(key: String, defaultValue: Long): Long {
        return map[key] as? Long ?: defaultValue
    }

    fun putFloat(key: String, value: Float) {
        map[key] = value
    }

    fun getFloat(key: String, defaultValue: Float): Float {
        return map[key] as? Float ?: defaultValue
    }

    fun putDouble(key: String, value: Double) {
        map[key] = value
    }

    fun getDouble(key: String, defaultValue: Double): Double {
        return map[key] as? Double ?: defaultValue
    }

    fun putBoolean(key: String, value: Boolean) {
        map[key] = value
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return map[key] as? Boolean ?: defaultValue
    }

    fun putParcelable(key: String, value: Parcelable?) {
        map[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Parcelable> getParcelable(key: String): T? {
        return map[key] as? T
    }
}
