/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.skillmarket.service;

import com.huawei.opsfactory.skillmarket.common.error.ApiConflictException;
import com.huawei.opsfactory.skillmarket.config.SkillMarketProperties;
import com.huawei.opsfactory.skillmarket.model.CreateSkillRequest;
import com.huawei.opsfactory.skillmarket.model.SkillDetail;
import com.huawei.opsfactory.skillmarket.model.SkillMutationResponse;
import com.huawei.opsfactory.skillmarket.model.SkillSummary;
import com.huawei.opsfactory.skillmarket.model.SkillWarning;
import com.huawei.opsfactory.skillmarket.model.UpdateSkillRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

@Service
/**
 * Skill Catalog Service.
 *
 * @author x00000000
 * @since 2026-05-27
 */
public class SkillCatalogService {

    private static final Logger log = LoggerFactory.getLogger(SkillCatalogService.class);
    private static final Pattern SKILL_ID_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,62}[a-z0-9])?$");
    private static final Set<String> SCRIPT_EXTENSIONS = Set.of(
        ".sh", ".py", ".js", ".ts", ".mjs", ".cjs", ".bash", ".zsh", ".ps1", ".bat"
    );

    private final SkillMarketProperties properties;
    private final Yaml yaml = new Yaml();

    public SkillCatalogService(SkillMarketProperties properties) {
        this.properties = properties;
    }

    public List<SkillSummary> listSkills(String query) throws IOException {
        Path skillsDir = skillsDir();
        if (!Files.isDirectory(skillsDir)) {
            return List.of();
        }

        String normalizedQuery = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        try (Stream<Path> stream = Files.list(skillsDir)) {
            return stream
                .filter(Files::isDirectory)
                .map(this::readSummaryUnchecked)
                .filter(skill -> matchesQuery(skill, normalizedQuery))
                .sorted(Comparator.comparing(SkillSummary::updatedAt).reversed())
                .toList();
        }
    }

    public SkillDetail getSkill(String skillId) throws IOException {
        String id = validateSkillId(skillId);
        Path skillDir = skillDir(id);
        if (!Files.isDirectory(skillDir)) {
            throw new IllegalArgumentException("Skill '" + id + "' not found");
        }
        return toDetail(readMetadata(skillDir), skillDir);
    }

    public Path packagePath(String skillId) {
        String id = validateSkillId(skillId);
        Path path = skillDir(id).resolve("package.zip");
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Skill '" + id + "' package not found");
        }
        return path;
    }

    public SkillMutationResponse createSkill(CreateSkillRequest request) throws IOException {
        return createSkill(request, false);
    }

    public SkillMutationResponse createSkillIfAbsent(CreateSkillRequest request) throws IOException {
        return createSkill(request, true);
    }

    private SkillMutationResponse createSkill(CreateSkillRequest request, boolean skipIfExists) throws IOException {
        String id = validateSkillId(request.id());
        if (skipIfExists && Files.exists(skillDir(id))) {
            return new SkillMutationResponse(readSummaryUnchecked(skillDir(id)), List.of());
        }
        ensureSkillDoesNotExist(id);

        Path tempDir = Files.createTempDirectory("skill-market-create-");
        try {
            Path unpacked = tempDir.resolve("unpacked");
            Files.createDirectories(unpacked);
            String skillMd = buildSkillMarkdown(request.name(), request.description(), request.instructions());
            Files.writeString(unpacked.resolve("SKILL.md"), skillMd);

            Path packagePath = tempDir.resolve("package.zip");
            writeZip(unpacked, packagePath);
            SkillSummary summary = persistSkill(id, request.name(), request.description(), false, tempDir);
            log.info("Created skill id={} checksum={} sizeBytes={}", id, summary.checksum(), summary.sizeBytes());
            return new SkillMutationResponse(summary, List.of());
        } finally {
            deleteRecursively(tempDir);
        }
    }

    public SkillMutationResponse updateSkill(String skillId, UpdateSkillRequest request) throws IOException {
        String id = validateSkillId(skillId);
        Path currentDir = skillDir(id);
        if (!Files.isDirectory(currentDir)) {
            throw new IllegalArgumentException("Skill '" + id + "' not found");
        }
        Map<String, Object> currentMetadata = readMetadata(currentDir);
        String createdAt = stringValue(currentMetadata, "createdAt");
        Path tempDir = Files.createTempDirectory("skill-market-update-");
        try {
            Path unpacked = tempDir.resolve("unpacked");
            copyRecursively(currentDir.resolve("unpacked"), unpacked);
            String skillMd = buildSkillMarkdown(request.name(), request.description(), request.instructions());
            Files.writeString(unpacked.resolve("SKILL.md"), skillMd);
            writeZip(unpacked, tempDir.resolve("package.zip"));
            SkillSummary summary = replaceSkill(id, request.name(), request.description(), hasScripts(unpacked), createdAt, tempDir);
            log.info("Updated skill id={} checksum={} sizeBytes={}", id, summary.checksum(), summary.sizeBytes());
            return new SkillMutationResponse(summary, List.of());
        } finally {
            deleteRecursively(tempDir);
        }
    }

    public SkillMutationResponse importSkill(MultipartFile file, String requestedId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Skill package file is required");
        }
        if (!isZipUpload(file)) {
            throw new IllegalArgumentException("Skill package must be a .zip file");
        }
        long maxUploadBytes = mbToBytes(properties.getPackage().getMaxUploadSizeMb());
        if (file.getSize() > maxUploadBytes) {
            throw new IllegalArgumentException("Skill package exceeds max upload size");
        }

        Path tempDir = Files.createTempDirectory("skill-market-import-");
        try {
            Path upload = tempDir.resolve("upload.zip");
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, upload, StandardCopyOption.REPLACE_EXISTING);
            }

            ImportPlan plan = inspectZip(upload);
            Path unpacked = tempDir.resolve("unpacked");
            Files.createDirectories(unpacked);
            ExtractStats stats = extractZip(upload, unpacked, plan.rootPrefix());
            Path skillMd = unpacked.resolve("SKILL.md");
            if (!Files.isRegularFile(skillMd) || Files.size(skillMd) == 0) {
                throw new IllegalArgumentException("Skill package must contain a non-empty SKILL.md");
            }

            Map<String, String> frontmatter = parseFrontmatter(skillMd);
            String id = resolveSkillId(requestedId, plan.rootName(), frontmatter.get("name"));
            ensureSkillDoesNotExist(id);
            String name = StringUtils.hasText(frontmatter.get("name")) ? frontmatter.get("name").trim() : id;
            String description = StringUtils.hasText(frontmatter.get("description"))
                ? frontmatter.get("description").trim()
                : "";

            writeZip(unpacked, tempDir.resolve("package.zip"));
            SkillSummary summary = persistSkill(id, name, description, stats.containsScripts(), tempDir);
            List<SkillWarning> warnings = stats.containsScripts()
                ? List.of(new SkillWarning("CONTAINS_SCRIPTS", "Skill package contains executable script files."))
                : List.of();
            log.info(
                "Imported skill id={} checksum={} fileCount={} containsScripts={}",
                id,
                summary.checksum(),
                summary.fileCount(),
                summary.containsScripts()
            );
            return new SkillMutationResponse(summary, warnings);
        } finally {
            deleteRecursively(tempDir);
        }
    }

    public void deleteSkill(String skillId) throws IOException {
        String id = validateSkillId(skillId);
        Path skillDir = skillDir(id);
        if (!Files.isDirectory(skillDir)) {
            throw new IllegalArgumentException("Skill '" + id + "' not found");
        }
        deleteRecursively(skillDir);
        log.info("Deleted skill id={}", id);
    }

    private SkillSummary persistSkill(
        String id,
        String name,
        String description,
        boolean containsScripts,
        Path tempDir
    ) throws IOException {
        Path destination = skillDir(id);
        ensureSkillDoesNotExist(id);
        Files.createDirectories(destination.getParent());

        Path packagePath = tempDir.resolve("package.zip");
        String checksum = sha256(packagePath);
        long sizeBytes = Files.size(packagePath);
        int fileCount = countFiles(tempDir.resolve("unpacked"));
        String now = Instant.now().toString();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("id", id);
        metadata.put("name", name);
        metadata.put("description", description == null ? "" : description);
        metadata.put("path", "skills/" + id);
        metadata.put("containsScripts", containsScripts);
        metadata.put("checksum", "sha256:" + checksum);
        metadata.put("sizeBytes", sizeBytes);
        metadata.put("fileCount", fileCount);
        metadata.put("createdAt", now);
        metadata.put("updatedAt", now);

        Path staged = Files.createTempDirectory(destination.getParent(), id + "-");
        try {
            Files.move(tempDir.resolve("unpacked"), staged.resolve("unpacked"), StandardCopyOption.ATOMIC_MOVE);
            Files.move(packagePath, staged.resolve("package.zip"), StandardCopyOption.ATOMIC_MOVE);
            writeMetadata(staged, metadata);
            Files.move(staged, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            deleteRecursively(staged);
            throw e;
        }
        return toSummary(metadata);
    }

    private SkillSummary replaceSkill(
        String id,
        String name,
        String description,
        boolean containsScripts,
        String createdAt,
        Path tempDir
    ) throws IOException {
        Path destination = skillDir(id);
        Path packagePath = tempDir.resolve("package.zip");
        String checksum = sha256(packagePath);
        long sizeBytes = Files.size(packagePath);
        int fileCount = countFiles(tempDir.resolve("unpacked"));
        String now = Instant.now().toString();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("id", id);
        metadata.put("name", name);
        metadata.put("description", description == null ? "" : description);
        metadata.put("path", "skills/" + id);
        metadata.put("containsScripts", containsScripts);
        metadata.put("checksum", "sha256:" + checksum);
        metadata.put("sizeBytes", sizeBytes);
        metadata.put("fileCount", fileCount);
        metadata.put("createdAt", StringUtils.hasText(createdAt) ? createdAt : now);
        metadata.put("updatedAt", now);

        Path staged = Files.createTempDirectory(destination.getParent(), id + "-update-");
        Path backup = Files.createTempDirectory(destination.getParent(), id + "-backup-");
        deleteRecursively(backup);
        try {
            Files.move(tempDir.resolve("unpacked"), staged.resolve("unpacked"), StandardCopyOption.ATOMIC_MOVE);
            Files.move(packagePath, staged.resolve("package.zip"), StandardCopyOption.ATOMIC_MOVE);
            writeMetadata(staged, metadata);
            Files.move(destination, backup, StandardCopyOption.ATOMIC_MOVE);
            Files.move(staged, destination, StandardCopyOption.ATOMIC_MOVE);
            deleteRecursively(backup);
        } catch (IOException | RuntimeException e) {
            deleteRecursively(staged);
            if (Files.notExists(destination) && Files.exists(backup)) {
                Files.move(backup, destination, StandardCopyOption.ATOMIC_MOVE);
            } else {
                deleteRecursively(backup);
            }
            throw e;
        }
        return toSummary(metadata);
    }

    private ImportPlan inspectZip(Path zipPath) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String safeName = safeZipName(entry.getName());
                if (shouldIgnoreZipEntry(safeName)) {
                    continue;
                }
                names.add(safeName);
            }
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException("Skill package is empty");
        }

        String commonRoot = commonRoot(names);
        String rootPrefix = commonRoot == null ? "" : commonRoot + "/";
        boolean hasSkillMd = names.stream()
            .map(name -> rootPrefix.isEmpty() ? name : stripPrefix(name, rootPrefix))
            .anyMatch("SKILL.md"::equals);
        if (!hasSkillMd) {
            throw new IllegalArgumentException("Skill package must contain SKILL.md");
        }
        return new ImportPlan(rootPrefix, commonRoot);
    }

    private ExtractStats extractZip(Path zipPath, Path targetDir, String rootPrefix) throws IOException {
        int fileCount = 0;
        long totalBytes = 0;
        boolean containsScripts = false;
        long maxUnpackedBytes = mbToBytes(properties.getPackage().getMaxUnpackedSizeMb());
        long maxSingleFileBytes = mbToBytes(properties.getPackage().getMaxSingleFileSizeMb());

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String safeName = safeZipName(entry.getName());
                if (shouldIgnoreZipEntry(safeName)) {
                    continue;
                }
                String normalized = rootPrefix.isEmpty() ? safeName : stripPrefix(safeName, rootPrefix);
                if (normalized.isBlank()) {
                    continue;
                }
                safeZipName(normalized);
                fileCount++;
                if (fileCount > properties.getPackage().getMaxFileCount()) {
                    throw new IllegalArgumentException("Skill package contains too many files");
                }
                if (hasScriptExtension(normalized)) {
                    containsScripts = true;
                }

                Path destination = targetDir.resolve(normalized).normalize();
                if (!destination.startsWith(targetDir)) {
                    throw new IllegalArgumentException("Skill package contains unsafe file path");
                }
                Files.createDirectories(destination.getParent());
                long entryBytes = copyWithLimits(zis, destination, maxSingleFileBytes);
                totalBytes += entryBytes;
                if (totalBytes > maxUnpackedBytes) {
                    throw new IllegalArgumentException("Skill package unpacked size exceeds configured limit");
                }
            }
        }

        rejectSymbolicLinks(targetDir);
        return new ExtractStats(fileCount, totalBytes, containsScripts);
    }

    private long copyWithLimits(InputStream in, Path destination, long maxSingleFileBytes) throws IOException {
        long written = 0;
        byte[] buffer = new byte[8192];
        try (OutputStream out = Files.newOutputStream(destination)) {
            int read;
            while ((read = in.read(buffer)) >= 0) {
                written += read;
                if (written > maxSingleFileBytes) {
                    throw new IllegalArgumentException("Skill package contains a file that exceeds configured limit");
                }
                out.write(buffer, 0, read);
            }
        }
        return written;
    }

    private String resolveSkillId(String requestedId, String rootName, String frontmatterName) {
        if (StringUtils.hasText(requestedId)) {
            return validateSkillId(requestedId.trim());
        }
        if (StringUtils.hasText(rootName)) {
            return validateSkillId(slugify(rootName));
        }
        if (StringUtils.hasText(frontmatterName)) {
            return validateSkillId(slugify(frontmatterName));
        }
        throw new IllegalArgumentException("Skill id is required when package has no root directory or frontmatter name");
    }

    private String validateSkillId(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Skill id is required");
        }
        String id = value.trim();
        if (!SKILL_ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Skill id must use lowercase letters, numbers, and hyphens");
        }
        return id;
    }

    private void ensureSkillDoesNotExist(String id) {
        if (Files.exists(skillDir(id))) {
            throw new ApiConflictException("SKILL_ALREADY_EXISTS", "Skill '" + id + "' already exists.");
        }
    }

    private String buildSkillMarkdown(String name, String description, String instructions) {
        String strippedInstructions = instructions == null ? "" : instructions.strip();
        if (strippedInstructions.startsWith("---")) {
            strippedInstructions = stripFrontmatter(strippedInstructions);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("---\n");
        builder.append("name: ").append(quoteYaml(name)).append('\n');
        builder.append("description: ").append(quoteYaml(description == null ? "" : description)).append('\n');
        builder.append("---\n\n");
        builder.append(strippedInstructions).append('\n');
        return builder.toString();
    }

    private String quoteYaml(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private Map<String, String> parseFrontmatter(Path skillMd) throws IOException {
        String content = Files.readString(skillMd);
        if (!content.startsWith("---")) {
            return Map.of();
        }
        int endIndex = frontmatterEndIndex(content);
        if (endIndex < 0) {
            return Map.of();
        }
        Object parsed = yaml.load(content.substring(3, endIndex).trim());
        if (!(parsed instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        return result;
    }

    private String stripFrontmatter(String content) {
        int endIndex = frontmatterEndIndex(content);
        if (endIndex < 0) {
            return content;
        }
        int bodyStart = endIndex + 3;
        if (bodyStart < content.length() && content.charAt(bodyStart) == '\r') {
            bodyStart++;
        }
        if (bodyStart < content.length() && content.charAt(bodyStart) == '\n') {
            bodyStart++;
        }
        return content.substring(bodyStart).strip();
    }

    private int frontmatterEndIndex(String content) {
        if (!content.startsWith("---")) {
            return -1;
        }
        int newlineEnd = content.indexOf("\n---", 3);
        if (newlineEnd >= 0) {
            return newlineEnd + 1;
        }
        return content.indexOf("---", 3);
    }

    private String safeZipName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new IllegalArgumentException("Skill package contains an empty file name");
        }
        String name = rawName.replace('\\', '/');
        if (name.startsWith("/") || name.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("Skill package contains absolute file path");
        }
        for (String part : name.split("/")) {
            if (part.equals("..")) {
                throw new IllegalArgumentException("Skill package contains unsafe parent path");
            }
        }
        return name;
    }

    private boolean shouldIgnoreZipEntry(String name) {
        return name.startsWith("__MACOSX/") || name.endsWith("/.DS_Store") || ".DS_Store".equals(name);
    }

    private String commonRoot(List<String> names) {
        String root = null;
        for (String name : names) {
            String[] parts = name.split("/");
            if (parts.length < 2) {
                return null;
            }
            if (root == null) {
                root = parts[0];
            } else if (!root.equals(parts[0])) {
                return null;
            }
        }
        return root;
    }

    private String stripPrefix(String name, String prefix) {
        if (!prefix.isEmpty() && name.startsWith(prefix)) {
            return name.substring(prefix.length());
        }
        return name;
    }

    private boolean hasScriptExtension(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return SCRIPT_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private boolean hasScripts(Path unpackedDir) throws IOException {
        if (!Files.isDirectory(unpackedDir)) {
            return false;
        }
        try (Stream<Path> stream = Files.walk(unpackedDir)) {
            return stream
                .filter(Files::isRegularFile)
                .map(unpackedDir::relativize)
                .map(path -> path.toString().replace('\\', '/'))
                .anyMatch(this::hasScriptExtension);
        }
    }

    private boolean isZipUpload(MultipartFile file) {
        String name = file.getOriginalFilename();
        String contentType = file.getContentType();
        return (name != null && name.toLowerCase(Locale.ROOT).endsWith(".zip"))
            || "application/zip".equalsIgnoreCase(contentType)
            || "application/x-zip-compressed".equalsIgnoreCase(contentType);
    }

    private String slugify(String value) {
        return value.toLowerCase(Locale.ROOT)
            .trim()
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "");
    }

    private boolean matchesQuery(SkillSummary skill, String query) {
        if (query.isBlank()) {
            return true;
        }
        return skill.id().toLowerCase(Locale.ROOT).contains(query)
            || skill.name().toLowerCase(Locale.ROOT).contains(query)
            || skill.description().toLowerCase(Locale.ROOT).contains(query);
    }

    private SkillSummary readSummaryUnchecked(Path skillDir) {
        try {
            return toSummary(readMetadata(skillDir));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read skill metadata from " + skillDir, e);
        }
    }

    private SkillDetail toDetail(Map<String, Object> metadata, Path skillDir) throws IOException {
        SkillSummary summary = toSummary(metadata);
        Path skillMd = skillDir.resolve("unpacked").resolve("SKILL.md");
        List<String> files = properties.getPackage().isExposeFileList()
            ? listUnpackedFiles(skillDir.resolve("unpacked"))
            : List.of();
        return new SkillDetail(
            summary.id(),
            summary.name(),
            summary.description(),
            summary.path(),
            summary.containsScripts(),
            summary.checksum(),
            summary.sizeBytes(),
            summary.fileCount(),
            "SKILL.md",
            files,
            Files.isRegularFile(skillMd) ? Files.readString(skillMd) : "",
            summary.createdAt(),
            summary.updatedAt()
        );
    }

    private SkillSummary toSummary(Map<String, Object> metadata) {
        return new SkillSummary(
            stringValue(metadata, "id"),
            stringValue(metadata, "name"),
            stringValue(metadata, "description"),
            stringValue(metadata, "path"),
            booleanValue(metadata, "containsScripts"),
            stringValue(metadata, "checksum"),
            longValue(metadata, "sizeBytes"),
            intValue(metadata, "fileCount"),
            stringValue(metadata, "createdAt"),
            stringValue(metadata, "updatedAt")
        );
    }

    private Map<String, Object> readMetadata(Path skillDir) throws IOException {
        Path metadataPath = skillDir.resolve("metadata.yaml");
        if (!Files.isRegularFile(metadataPath)) {
            throw new IllegalArgumentException("Skill metadata not found: " + skillDir.getFileName());
        }
        Object parsed = yaml.load(Files.readString(metadataPath));
        if (!(parsed instanceof Map<?, ?> map)) {
            throw new IllegalStateException("Invalid skill metadata: " + metadataPath);
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                metadata.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return metadata;
    }

    private void writeMetadata(Path skillDir, Map<String, Object> metadata) throws IOException {
        Files.writeString(skillDir.resolve("metadata.yaml"), yaml.dump(metadata));
    }

    private List<String> listUnpackedFiles(Path unpackedDir) throws IOException {
        if (!Files.isDirectory(unpackedDir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(unpackedDir)) {
            return stream
                .filter(Files::isRegularFile)
                .map(unpackedDir::relativize)
                .map(path -> path.toString().replace('\\', '/'))
                .sorted()
                .toList();
        }
    }

    private void writeZip(Path sourceDir, Path zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath));
             Stream<Path> stream = Files.walk(sourceDir)) {
            for (Path file : stream.filter(Files::isRegularFile).sorted().toList()) {
                String entryName = sourceDir.relativize(file).toString().replace('\\', '/');
                zos.putNextEntry(new ZipEntry(entryName));
                Files.copy(file, zos);
                zos.closeEntry();
            }
        }
    }

    private void copyRecursively(Path sourceDir, Path targetDir) throws IOException {
        if (!Files.isDirectory(sourceDir)) {
            throw new IllegalArgumentException("Skill unpacked directory not found: " + sourceDir);
        }
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceDir.relativize(dir);
                Files.createDirectories(targetDir.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (Files.isSymbolicLink(file)) {
                    throw new IllegalArgumentException("Skill package must not contain symbolic links");
                }
                Path relative = sourceDir.relativize(file);
                Files.copy(file, targetDir.resolve(relative), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new DigestInputStream(Files.newInputStream(path), digest)) {
                in.transferTo(OutputStream.nullOutputStream());
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private int countFiles(Path dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            return (int) stream.filter(Files::isRegularFile).count();
        }
    }

    private void rejectSymbolicLinks(Path dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            boolean hasSymlink = stream.anyMatch(Files::isSymbolicLink);
            if (hasSymlink) {
                throw new IllegalArgumentException("Skill package must not contain symbolic links");
            }
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (path == null || Files.notExists(path)) {
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private Path skillsDir() {
        return baseDir().resolve("skills");
    }

    private Path skillDir(String id) {
        return skillsDir().resolve(id);
    }

    private Path baseDir() {
        return Path.of(properties.getRuntime().getBaseDir()).toAbsolutePath().normalize();
    }

    private long mbToBytes(int mb) {
        return mb * 1024L * 1024L;
    }

    private String stringValue(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null ? "" : value.toString();
    }

    private boolean booleanValue(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
    }

    private long longValue(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private int intValue(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private record ImportPlan(String rootPrefix, String rootName) {
    }

    private record ExtractStats(int fileCount, long totalBytes, boolean containsScripts) {
    }
}
