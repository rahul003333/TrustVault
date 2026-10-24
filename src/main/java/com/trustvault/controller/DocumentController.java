package com.trustvault.controller;

import com.trustvault.entity.Document;
import com.trustvault.service.DocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'DOCUMENT_OWNER')")
    public Document uploadDocument(
            @RequestPart("file") MultipartFile file,
            Authentication authentication) throws IOException {

        return documentService.uploadDocument(
                authentication.getName(), file);
    }



    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'DOCUMENT_OWNER')")
    public List<Document> getMyDocuments(
            Authentication authentication) {

        return documentService.getMyDocuments(
                authentication.getName());
    }


    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'DOCUMENT_OWNER', 'VIEWER')")
    public List<Document> getAllDocuments(
            Authentication authentication) {

        return documentService.getVisibleDocuments(
                authentication.getName());
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','DOCUMENT_OWNER', 'VIEWER')")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            Authentication authentication) throws IOException {

        Resource resource = documentService.downloadMyDocument(
                authentication.getName(), id);

        Document document = findDocumentForResponse(id, authentication);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                safeFilename(document.getFileName()) + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','DOCUMENT_OWNER', 'VIEWER')")
    public ResponseEntity<Resource> previewDocument(
            @PathVariable Long id,
            Authentication authentication) throws IOException {

        Resource resource = documentService.downloadMyDocument(
                authentication.getName(), id);

        Document document = findDocumentForResponse(id, authentication);

        MediaType mediaType;

        try {
            mediaType = MediaType.parseMediaType(
                    document.getContentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        // Only render PDF and image files inline.
        if (!mediaType.equals(MediaType.APPLICATION_PDF)
                && !mediaType.getType().equalsIgnoreCase("image")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .build();
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" +
                                safeFilename(document.getFileName()) + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(mediaType)
                .body(resource);
    }

    @PutMapping("/{id}/visibility")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'DOCUMENT_OWNER')")
    public Document changeVisibility(
            @PathVariable Long id,
            @RequestParam boolean makePublic,
            Authentication authentication) {

        return documentService.setDocumentVisibility(
                authentication.getName(), id, makePublic);
    }

    /*
     * Retrieves the document metadata for response headers.
     * The service must still authorize file access before bytes are returned.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'DOCUMENT_OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable Long id,
            Authentication authentication) throws IOException{

        documentService.deleteMyDocument(
                authentication.getName(), id);
    }
    private Document findDocumentForResponse(
            Long id,
            Authentication authentication) {

        return documentService.getDocumentMetadataForAuthorizedUser(
                authentication.getName(), id);
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document";
        }

        return filename.replace("\"", "")
                .replace("\r", "")
                .replace("\n", "")
                .replace("/", "_")
                .replace("\\", "_");
    }
}