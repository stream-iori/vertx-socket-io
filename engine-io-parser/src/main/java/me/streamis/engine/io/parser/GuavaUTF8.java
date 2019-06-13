package me.streamis.engine.io.parser;

/**
 * This helper class copy from Guava.
 */
class GuavaUTF8 {
  /**
   * Returns whether the given byte array slice is a well-formed UTF-8 byte sequence, as defined by
   * {@link #isWellFormed(byte[])}. Note that this can be false even when {@code
   * isWellFormed(bytes)} is true.
   *
   * @param bytes the input buffer
   * @param off   the offset in the buffer of the first byte to read
   * @param len   the number of bytes to read from the buffer
   */
  static boolean isWellFormed(byte[] bytes, int off, int len) {
    int end = off + len;
    checkPositionIndexes(off, end, bytes.length);
    // Look for the first non-ASCII character.
    for (int i = off; i < end; i++) {
      if (bytes[i] < 0) {
        return isWellFormedSlowPath(bytes, i, end);
      }
    }
    return true;
  }

  /**
   * Ensures that {@code start} and {@code end} specify a valid <i>positions</i> in an array, list
   * or string of size {@code size}, and are in order. A position index may range from zero to
   * {@code size}, inclusive.
   *
   * @param start a user-supplied index identifying a starting position in an array, list or string
   * @param end a user-supplied index identifying a ending position in an array, list or string
   * @param size the size of that array, list or string
   * @throws IndexOutOfBoundsException if either index is negative or is greater than {@code size},
   *     or if {@code end} is less than {@code start}
   * @throws IllegalArgumentException if {@code size} is negative
   */
  /**
   * Ensures that {@code start} and {@code end} specify a valid <i>positions</i> in an array, list
   * or string of size {@code size}, and are in order. A position index may range from zero to
   * {@code size}, inclusive.
   *
   * @param start a user-supplied index identifying a starting position in an array, list or string
   * @param end   a user-supplied index identifying a ending position in an array, list or string
   * @param size  the size of that array, list or string
   * @throws IndexOutOfBoundsException if either index is negative or is greater than {@code size},
   *                                   or if {@code end} is less than {@code start}
   * @throws IllegalArgumentException  if {@code size} is negative
   */
  private static void checkPositionIndexes(int start, int end, int size) {
    // Carefully optimized for execution by hotspot (explanatory comment above)
    if (start < 0 || end < start || end > size) {
      throw new IndexOutOfBoundsException(badPositionIndexes(start, end, size));
    }
  }

  private static String badPositionIndexes(int start, int end, int size) {
    if (start < 0 || start > size) {
      return badPositionIndex(start, size, "start index");
    }
    if (end < 0 || end > size) {
      return badPositionIndex(end, size, "end index");
    }
    // end < start
    return lenientFormat("end index (%s) must not be less than start index (%s)", end, start);
  }

  private static String badPositionIndex(int index, int size, String desc) {
    if (index < 0) {
      return lenientFormat("%s (%s) must not be negative", desc, index);
    } else if (size < 0) {
      throw new IllegalArgumentException("negative size: " + size);
    } else { // index > size
      return lenientFormat("%s (%s) must not be greater than size (%s)", desc, index, size);
    }
  }

  private static boolean isWellFormedSlowPath(byte[] bytes, int off, int end) {
    int index = off;
    while (true) {
      int byte1;

      // Optimize for interior runs of ASCII bytes.
      do {
        if (index >= end) {
          return true;
        }
      } while ((byte1 = bytes[index++]) >= 0);

      if (byte1 < (byte) 0xE0) {
        // Two-byte form.
        if (index == end) {
          return false;
        }
        // Simultaneously check for illegal trailing-byte in leading position
        // and overlong 2-byte form.
        if (byte1 < (byte) 0xC2 || bytes[index++] > (byte) 0xBF) {
          return false;
        }
      } else if (byte1 < (byte) 0xF0) {
        // Three-byte form.
        if (index + 1 >= end) {
          return false;
        }
        int byte2 = bytes[index++];
        if (byte2 > (byte) 0xBF
          // Overlong? 5 most significant bits must not all be zero.
          || (byte1 == (byte) 0xE0 && byte2 < (byte) 0xA0)
          // Check for illegal surrogate codepoints.
          || (byte1 == (byte) 0xED && (byte) 0xA0 <= byte2)
          // Third byte trailing-byte test.
          || bytes[index++] > (byte) 0xBF) {
          return false;
        }
      } else {
        // Four-byte form.
        if (index + 2 >= end) {
          return false;
        }
        int byte2 = bytes[index++];
        if (byte2 > (byte) 0xBF
          // Check that 1 <= plane <= 16. Tricky optimized form of:
          // if (byte1 > (byte) 0xF4
          //     || byte1 == (byte) 0xF0 && byte2 < (byte) 0x90
          //     || byte1 == (byte) 0xF4 && byte2 > (byte) 0x8F)
          || (((byte1 << 28) + (byte2 - (byte) 0x90)) >> 30) != 0
          // Third byte trailing-byte test
          || bytes[index++] > (byte) 0xBF
          // Fourth byte trailing-byte test
          || bytes[index++] > (byte) 0xBF) {
          return false;
        }
      }
    }
  }

  /**
   * Returns the given {@code template} string with each occurrence of {@code "%s"} replaced with
   * the corresponding argument value from {@code args}; or, if the placeholder and argument counts
   * do not match, returns a best-effort form of that string. Will not throw an exception under any
   * circumstances (as long as all arguments' {@code toString} methods successfully return).
   *
   * <p><b>Note:</b> For most string-formatting needs, use {@link String#format}, {@link
   * PrintWriter#format}, and related methods. These support the full range of {@linkplain
   * Formatter#syntax format specifiers}, and alert you to usage errors by throwing {@link
   * InvalidFormatException}.
   *
   * <p>In certain cases, such as outputting debugging information or constructing a message to be
   * used for another unchecked exception, an exception during string formatting would serve little
   * purpose except to supplant the real information you were trying to provide. These are the cases
   * this method is made for; it instead generates a best-effort string with all supplied argument
   * values present. This method is also useful in environments such as GWT where {@code
   * String.format} is not available. As an example, method implementations of the {@link
   * Preconditions} class use this formatter, for both of the reasons just discussed.
   *
   * <p><b>Warning:</b> Only the exact two-character placeholder sequence {@code "%s"} is
   * recognized.
   *
   * @param template a string containing zero or more {@code "%s"} placeholder sequences. {@code
   *                 null} is treated as the four-character string {@code "null"}.
   * @param args     the arguments to be substituted into the message template. The first argument
   *                 specified is substituted for the first occurrence of {@code "%s"} in the template, and so
   *                 forth. A {@code null} argument is converted to the four-character string {@code "null"};
   *                 non-null values are converted to strings using {@link Object#toString()}.
   * @since 25.1
   */
  // TODO(diamondm) consider using Arrays.toString() for array parameters
  // TODO(diamondm) capture exceptions thrown from arguments' toString methods
  private static String lenientFormat(String template, Object... args) {
    template = String.valueOf(template); // null -> "null"

    args = args == null ? new Object[]{"(Object[])null"} : args;

    // start substituting the arguments into the '%s' placeholders
    StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
    int templateStart = 0;
    int i = 0;
    while (i < args.length) {
      int placeholderStart = template.indexOf("%s", templateStart);
      if (placeholderStart == -1) {
        break;
      }
      builder.append(template, templateStart, placeholderStart);
      builder.append(args[i++]);
      templateStart = placeholderStart + 2;
    }
    builder.append(template, templateStart, template.length());

    // if we run out of placeholders, append the extra args in square braces
    if (i < args.length) {
      builder.append(" [");
      builder.append(args[i++]);
      while (i < args.length) {
        builder.append(", ");
        builder.append(args[i++]);
      }
      builder.append(']');
    }

    return builder.toString();
  }
}
