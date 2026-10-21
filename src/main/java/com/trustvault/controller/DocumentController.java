
package com.trustvault.controller;

import com.trustvault.entity.Document;
import com.trustvault.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DOCUMENT_OWNER')")
    public Document uploadDocument(
            @RequestPart("file") MultipartFile file,
            Authentication authentication) throws IOException {

        return documentService.uploadDocument(
                authentication.getName(), file);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('DOCUMENT_OWNER')")
    public List<Document> getMyDocuments(Authentication authentication) {
        return documentService.getMyDocuments(
                authentication.getName());
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('DOCUMENT_OWNER')")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            Authentication authentication) throws IOException {

        Resource resource = documentService.downloadMyDocument(
                authentication.getName(), id);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

}
