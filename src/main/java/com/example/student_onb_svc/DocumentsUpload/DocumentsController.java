package com.example.student_onb_svc.DocumentsUpload;

import com.example.student_onb_svc.Security.StudentPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/onboarding")
public class DocumentsController {

    @Autowired
    DocumentsService documentService;
    @GetMapping("/documents")
    public ResponseEntity<List<Map<String, Object>>> getDocuments(
            @RequestParam String token) {
        return ResponseEntity.ok(documentService.getAll(token));
    }

    @PostMapping("/documents/upload-all")
    public ResponseEntity<Void> uploadAllDocuments(
            @RequestParam String token,
            @RequestParam Map<String, MultipartFile> files) {
        documentService.uploadAll(token, files);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/documents/upload")
    public ResponseEntity<Void> uploadSingleDocument(
            @RequestParam String token,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file) {
        documentService.uploadSingle(token, documentType, file);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @AuthenticationPrincipal StudentPrincipal principal,
            @PathVariable UUID documentId) {
        documentService.delete(principal.getStudentId(), documentId);
        return ResponseEntity.ok().build();
    }
}
