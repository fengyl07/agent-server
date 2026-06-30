package uyun.eagle.agent.alertagent.knowledge.index;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 文档切块器（Phase 1d）。
 *
 * <p>先按 Markdown 标题（{@code #}/{@code ##}…）划分小节，保留每个片段所属标题；
 * 小节正文超过 {@code chunkSize} 时再按字符窗口切分并带 {@code overlap} 重叠，
 * 以兼顾语义完整与检索粒度。切块逻辑无状态，便于单测与复用。
 */
public final class DocumentChunker {

    private DocumentChunker() {
    }

    /** 切块结果：正文 + 所属标题。 */
    public static class Chunk {
        private final String content;
        private final String title;

        public Chunk(String content, String title) {
            this.content = content;
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public String getTitle() {
            return title;
        }
    }

    /**
     * 把整篇 Markdown 切成片段。
     *
     * @param markdown 文档全文
     * @param chunkSize 目标片段长度（字符）
     * @param overlap 相邻片段重叠长度（字符）
     * @return 片段列表（不含空白片段）
     */
    public static List<Chunk> chunk(String markdown, int chunkSize, int overlap) {
        List<Chunk> chunks = new ArrayList<>();
        if (markdown == null || markdown.trim().isEmpty()) {
            return chunks;
        }
        int size = Math.max(100, chunkSize);
        int ov = Math.max(0, Math.min(overlap, size / 2));

        String[] lines = markdown.split("\r?\n", -1);
        String currentTitle = "";
        StringBuilder section = new StringBuilder();

        for (String line : lines) {
            if (isHeading(line)) {
                flushSection(chunks, section, currentTitle, size, ov);
                section.setLength(0);
                currentTitle = stripHeading(line);
            } else {
                section.append(line).append('\n');
            }
        }
        flushSection(chunks, section, currentTitle, size, ov);
        return chunks;
    }

    private static void flushSection(List<Chunk> chunks, StringBuilder section, String title, int size, int overlap) {
        String body = section.toString().trim();
        if (body.isEmpty()) {
            return;
        }
        // 标题并入正文，便于检索命中后片段自带上下文
        String prefix = (title == null || title.isEmpty()) ? "" : (title + "\n");
        String full = prefix + body;

        if (full.length() <= size) {
            chunks.add(new Chunk(full, title));
            return;
        }
        int start = 0;
        while (start < full.length()) {
            int end = Math.min(full.length(), start + size);
            String piece = full.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(new Chunk(piece, title));
            }
            if (end >= full.length()) {
                break;
            }
            start = end - overlap;
            if (start < 0) {
                start = 0;
            }
        }
    }

    private static boolean isHeading(String line) {
        if (line == null) {
            return false;
        }
        String t = line.trim();
        return t.startsWith("#");
    }

    private static String stripHeading(String line) {
        String t = line.trim();
        int i = 0;
        while (i < t.length() && t.charAt(i) == '#') {
            i++;
        }
        return t.substring(i).trim();
    }
}
