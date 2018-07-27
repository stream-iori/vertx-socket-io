package me.streamis;

import java.util.Random;

/**
 * Created by stream.
 */
class Helper {

  final private static byte[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
    '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
    'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y',
    'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
    'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y',
    'Z'};
  final private static Random random = new Random();

  static String randomSessionID() {
    byte[] cs = new byte[16];
    for (int i = 0; i < cs.length; i++) {
      cs[i] = digits[random.nextInt(digits.length)];
    }
    return new String(cs);
  }
}
