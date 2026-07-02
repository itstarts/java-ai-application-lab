package io.github.itstarts.aialab.chat.api;

public final class ChatMessageNormalizer {

    private ChatMessageNormalizer() {
    }

    public static String trim(String value) {
        if (value == null) {
            return null;
        }

        int start = 0;
        int end = value.length();
        while (start < end) {
            int codePoint = value.codePointAt(start);
            if (!isBlankCodePoint(codePoint)) {
                break;
            }
            start += Character.charCount(codePoint);
        }
        while (start < end) {
            int codePoint = value.codePointBefore(end);
            if (!isBlankCodePoint(codePoint)) {
                break;
            }
            end -= Character.charCount(codePoint);
        }
        return value.substring(start, end);
    }

    public static boolean hasText(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = trim(value);
        return !trimmed.isEmpty();
    }

    private static boolean isBlankCodePoint(int codePoint) {
        // isSpaceChar 覆盖 NBSP、Figure Space 等 isWhitespace 不识别的 Unicode 空格分隔符。
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }
}
