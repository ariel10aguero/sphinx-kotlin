package chat.sphinx.wrapper_relay

inline class JavaWebToken(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "JavaWebToken cannot be empty"
        }
    }
}