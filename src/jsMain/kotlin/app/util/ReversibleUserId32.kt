package app.util

/**
 * Reversible 32-bit encoding for constrained usernames.
 *
 * Constraints (enforced):
 * - Alphabet size: 32 symbols => 5 bits per character.
 * - Max length: 5 characters (length stored in top 3 bits).
 * - Characters allowed: 'A'..'Z' and '0'..'5' (32 total).
 *
 * Bit layout (big picture):
 * - Bits 31..29: lengthMinusOne (0..4) => actual length = lengthMinusOne + 1
 * - Bits 0..(5*len-1): 5-bit codes for each character, packed LSB-first (char0 at LSB).
 * - All other bits are zero.
 *
 * This is *bijective* within the constrained space: encode -> decode yields the same string.
 *
 * NOTE: If you need more characters or longer names, 32 bits isn’t enough for reversibility
 * without a registry/lookup. Consider using 64-bit IDs or store a server-side mapping.
 */
object ReversibleUserId32 {
    // 32-symbol alphabet (5 bits per char). Adjust ONLY if you regenerate existing IDs.
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ012345"
    private const val MAX_LEN = 5
    private const val LENGTH_BITS = 3 // supports values 0..7 (we only use 0..4)
    private const val CHAR_BITS = 5   // 32 symbols

    private val charToCode: IntArray = IntArray(128) { -1 }.apply {
        for (i in ALPHABET.indices) {
            val c = ALPHABET[i]
            require(c.code < 128) { "Alphabet must be ASCII for this implementation." }
            this[c.code] = i
        }
    }

    /**
     * Encode a username into a single Int.
     * - Input must be 1..5 chars, each in ALPHABET (case-insensitive letters will be uppercased).
     * - Throws IllegalArgumentException if constraints are violated.
     */
    fun encode(usernameRaw: String): Int {
        require(usernameRaw.isNotEmpty()) { "Username must not be empty." }
        val username = usernameRaw.trim().uppercase()
        require(username.length in 1..MAX_LEN) {
            "Username length must be between 1 and $MAX_LEN (got ${username.length})."
        }

        var bits = 0
        // Pack characters into bits starting from LSB, 5 bits per character.
        // char 0 (first) -> lowest 5 bits; char 1 -> next 5 bits; etc.
        for ((idx, ch) in username.withIndex()) {
            val code = ch.code.takeIf { it < 128 }?.let { charToCode[it] } ?: -1
            require(code >= 0) {
                "Unsupported character '$ch'. Allowed: $ALPHABET"
            }
            bits = bits or (code and 0x1F shl (idx * CHAR_BITS))
        }

        // Store lengthMinusOne in top 3 bits (bits 31..29).
        val lengthMinusOne = username.length - 1
        require(lengthMinusOne in 0 until (1 shl LENGTH_BITS)) // 0..7
        bits = bits or (lengthMinusOne shl 29)

        return bits
    }

    /**
     * Decode a 32-bit Int back into the username.
     * - Validates that all unused bits above the encoded length are zero (canonical form).
     * - Throws IllegalArgumentException if the code is not a canonical encoding.
     */
    fun decode(packed: Int): String {
        val lengthMinusOne = (packed ushr 29) and 0b111
        val length = lengthMinusOne + 1
        require(length in 1..MAX_LEN) { "Invalid length field in packed ID." }

        val sb = StringBuilder(length)
        // Extract 5-bit codes LSB-first.
        for (idx in 0 until length) {
            val code = (packed ushr (idx * CHAR_BITS)) and 0x1F
            require(code in ALPHABET.indices) { "Invalid character code in packed ID." }
            sb.append(ALPHABET[code])
        }

        // Canonicality check: ensure all higher char slots are zero-coded.
        // Any bits beyond 5*length up to bit 28 must be zero in canonical form.
        val usedBits = length * CHAR_BITS
        val maskUsed = if (usedBits == 32) -1 else ((1 shl usedBits) - 1)
        val charBitsOnly = packed and 0x1FFF_FFFF // lower 29 bits (chars region)
        val unusedBits = charBitsOnly and maskUsed.inv()
        require(unusedBits == 0) {
            "Packed ID has non-canonical extra bits set."
        }

        return sb.toString()
    }

    /**
     * Quick helpers if you want to expose a friendlier API.
     */
    fun tryEncode(username: String): Result<Int> =
        runCatching { encode(username) }

    fun tryDecode(id: Int): Result<String> =
        runCatching { decode(id) }
}
