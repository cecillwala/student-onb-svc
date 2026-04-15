package com.example.student_onb_svc.DocumentsUpload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.example.student_onb_svc.Common.FileStorageService;
import com.example.student_onb_svc.Common.Helper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentsService {

    private final JdbcTemplate jdbc;
    private final FileStorageService fileStorage;

    @Autowired
    Helper helper;

    /**
     * Get all documents for a student.
     */
    public List<Map<String, Object>> getAll(String token) {

        UUID studentId = helper.getStudentId(token);

        return jdbc.query("""
            SELECT id, document_type, file_name, file_size, mime_type, status, flag_reason, uploaded_at
            FROM documents
            WHERE student_id = ?
            ORDER BY uploaded_at
            """,
                (rs, rowNum) -> Map.<String, Object>of(
                        "id", rs.getObject("id", UUID.class).toString(),
                        "documentType", rs.getString("document_type"),
                        "fileName", rs.getString("file_name"),
                        "fileSize", rs.getLong("file_size"),
                        "mimeType", rs.getString("mime_type") != null ? rs.getString("mime_type") : "",
                        "status", rs.getString("status"),
                        "uploadedAt", rs.getTimestamp("uploaded_at").toString()
                ), studentId
        );
    }

    /**
     * Upload multiple documents in one request.
     * Each file is keyed by its document type name.
     */
    public void uploadAll(String token, Map<String, MultipartFile> files) {

        UUID studentId = helper.getStudentId(token);

        for (Map.Entry<String, MultipartFile> entry : files.entrySet()) {
            String documentType = sanitizeDocumentType(entry.getKey());
            MultipartFile file = entry.getValue();

            String path = fileStorage.saveFile(studentId, "documents", file);

            // Delete existing document of this type (replace on re-upload)
            jdbc.update("DELETE FROM documents WHERE student_id = ? AND document_type = ?",
                    studentId, documentType);

            // Insert new
            jdbc.update("""
                INSERT INTO documents (id, student_id, document_type, file_path, file_name, file_size, mime_type)
                VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?)
                """,
                    studentId, documentType, path,
                    file.getOriginalFilename(), file.getSize(), file.getContentType()
            );

            log.info("Document uploaded: {} -> {} for student {}", documentType, file.getOriginalFilename(), studentId);
        }

        // Advance step
        jdbc.update("""
            UPDATE students SET current_step = GREATEST(current_step, 6), updated_at = NOW()
            WHERE id = ?
            """, studentId);
    }

    /**
     * Upload a single document.
     */
    public void uploadSingle(String token, String documentType, MultipartFile file) {

        UUID studentId = helper.getStudentId(token);
        String type = sanitizeDocumentType(documentType);
        String path = fileStorage.saveFile(studentId, "documents", file);

        // Replace if exists
        jdbc.update("DELETE FROM documents WHERE student_id = ? AND document_type = ?",
                studentId, type);

        jdbc.update("""
            INSERT INTO documents (id, student_id, document_type, file_path, file_name, file_size, mime_type)
            VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?)
            """,
                studentId, type, path,
                file.getOriginalFilename(), file.getSize(), file.getContentType()
        );

        log.info("Document uploaded: {} for student {}", type, studentId);
    }

    /**
     * Delete a document by ID.
     */
    public void delete(UUID studentId, UUID documentId) {
        int deleted = jdbc.update(
                "DELETE FROM documents WHERE id = ? AND student_id = ?",
                documentId, studentId);

        if (deleted == 0) {
            throw new IllegalArgumentException("Document not found");
        }

        log.info("Document deleted: {} for student {}", documentId, studentId);
    }

    /**
     * Convert display name to DB-safe type.
     * e.g., "KCSE Certificate" → "KCSE_CERTIFICATE"
     */
    private String sanitizeDocumentType(String name) {
        return name.toUpperCase().replaceAll("[^A-Z0-9]", "_").replaceAll("_+", "_");
    }
}