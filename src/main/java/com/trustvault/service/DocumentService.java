
package com.trustvault.service;

import com.trustvault.entity.Document;
import com.trustvault.entity.Role;
import com.trustvault.entity.User;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final Path storageDirectory;

    public DocumentService(
            DocumentRepository documentRepository,
            UserRepository userRepository,
            AuditService auditService,
            @Value("${trustvault.storage-dir:./trustvault-files}")
            String storageDirectory) {

        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.storageDirectory = Paths.get(storageDirectory)
                .toAbsolutePath()
                .normalize();
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }


    @Transactional
    public Document uploadDocument(
            String username,
            MultipartFile file) throws IOException {

        User owner = getUser(username);

        // Allow only Administrators and Document Owners.
        if (owner.getRole() != Role.ADMINISTRATOR
                && owner.getRole() != Role.DOCUMENT_OWNER) {

            auditService.log(
                    username, "DOCUMENT_UPLOAD", "DOCUMENT",
                    null, "DENIED",
                    "Upload denied: user is not authorized to upload files"
            );

            throw new AccessDeniedException(
                    "Only Administrators and Document Owners can upload files");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select a non-empty file");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "File size must not exceed 10 MB");
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !(contentType.equals("application/pdf")
                        || contentType.equals("image/png")
                        || contentType.equals("image/jpeg")
                        || contentType.equals(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {

            throw new IllegalArgumentException(
                    "Only PDF, PNG, JPEG, and DOCX files are allowed");
        }

        Files.createDirectories(storageDirectory);

        String originalName = Paths.get(
                        file.getOriginalFilename() == null
                                ? "document"
                                : file.getOriginalFilename())
                .getFileName()
                .toString();

        String storedName = UUID.randomUUID() + ".bin";
        Path destination = storageDirectory.resolve(storedName).normalize();

        if (!destination.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        try {
            Files.copy(
                    file.getInputStream(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            Document document = new Document();
            document.setFileName(originalName);
            document.setContentType(contentType);
            document.setStoragePath(destination.toString());
            document.setOwnerId(owner.getId());
            document.setPubliclyShared(false);

            Document savedDocument = documentRepository.save(document);

            auditService.log(
                    username, "DOCUMENT_UPLOAD", "DOCUMENT",
                    savedDocument.getId(), "SUCCESS",
                    "Document uploaded successfully; private by default"
            );

            return savedDocument;

        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(destination);
            throw ex;
        }
    }



    @Transactional(readOnly = true)
    public List<Document> getMyDocuments(String username) {

        User user = getUser(username);

        if (user.getRole() != Role.ADMINISTRATOR
                && user.getRole() != Role.DOCUMENT_OWNER) {
            throw new AccessDeniedException(
                    "You are not authorized to view My Documents");
        }

        return documentRepository.findByOwnerId(user.getId());
    }


    @Transactional(readOnly = true)
    public List<Document> getVisibleDocuments(String username) {
        User user = getUser(username);

        if (user.getRole() == Role.ADMINISTRATOR) {
            return documentRepository.findAll();
        }

        if (user.getRole() == Role.DOCUMENT_OWNER) {
            return documentRepository
                    .findByOwnerIdOrPubliclySharedTrue(user.getId());
        }

        if (user.getRole() == Role.VIEWER) {
            return documentRepository.findByPubliclySharedTrue();
        }

        throw new AccessDeniedException("Access denied");
    }

    @Transactional
    public Document setDocumentVisibility(
            String username,
            Long documentId,
            boolean makePublic) {

        User owner = getUser(username);

        if (owner.getRole() != Role.DOCUMENT_OWNER) {
            auditService.log(
                    username, "DOCUMENT_VISIBILITY_CHANGE", "DOCUMENT",
                    documentId, "DENIED",
                    "Only Document Owners can change visibility"
            );
            throw new AccessDeniedException(
                    "Only Document Owners can change visibility");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Document not found"));

        if (!document.getOwnerId().equals(owner.getId())) {
            auditService.log(
                    username, "DOCUMENT_VISIBILITY_CHANGE", "DOCUMENT",
                    documentId, "DENIED",
                    "Cannot change another owner's document visibility"
            );
            throw new AccessDeniedException(
                    "You cannot change another owner's document visibility");
        }

        document.setPubliclyShared(makePublic);
        Document savedDocument = documentRepository.save(document);

        auditService.log(
                username, "DOCUMENT_VISIBILITY_CHANGE", "DOCUMENT",
                documentId, "SUCCESS",
                makePublic ? "Document made public" : "Document made private"
        );

        return savedDocument;
    }


    @Transactional(readOnly = true)
    public Resource downloadMyDocument(
            String username,
            Long documentId) throws MalformedURLException {

        User user = getUser(username);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Document not found"));

        boolean isOwner =
                (user.getRole() == Role.ADMINISTRATOR
                        || user.getRole() == Role.DOCUMENT_OWNER)
                        && document.getOwnerId().equals(user.getId());

        boolean canAccessPublic =
                document.isPubliclyShared()
                        && (user.getRole() == Role.ADMINISTRATOR
                        || user.getRole() == Role.DOCUMENT_OWNER
                        || user.getRole() == Role.VIEWER);

        boolean allowed = isOwner || canAccessPublic;

        if (!allowed) {
            auditService.log(
                    username, "DOCUMENT_DOWNLOAD", "DOCUMENT",
                    documentId, "DENIED",
                    "Download denied: document is private or access is not permitted"
            );

            throw new AccessDeniedException(
                    "You are not allowed to download this document");
        }

        Path filePath = Paths.get(document.getStoragePath())
                .toAbsolutePath()
                .normalize();

        if (!filePath.startsWith(storageDirectory.toAbsolutePath().normalize())
                || !Files.isRegularFile(filePath)
                || !Files.isReadable(filePath)) {
            throw new IllegalArgumentException(
                    "Document file is unavailable");
        }

        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException(
                    "Document file is unavailable");
        }

        auditService.log(
                username, "DOCUMENT_DOWNLOAD", "DOCUMENT",
                documentId, "SUCCESS",
                "Document download requested successfully"
        );

        return resource;
    }


    @Transactional(readOnly = true)
    public Document getDocumentMetadataForAuthorizedUser(
            String username,
            Long documentId) {

        User user = getUser(username);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Document not found"));

        boolean isOwner =
                (user.getRole() == Role.ADMINISTRATOR
                        || user.getRole() == Role.DOCUMENT_OWNER)
                        && document.getOwnerId().equals(user.getId());

        boolean canAccessPublic =
                document.isPubliclyShared()
                        && (user.getRole() == Role.ADMINISTRATOR
                        || user.getRole() == Role.DOCUMENT_OWNER
                        || user.getRole() == Role.VIEWER);

        if (!isOwner && !canAccessPublic) {
            auditService.log(
                    username, "DOCUMENT_METADATA", "DOCUMENT",
                    documentId, "DENIED",
                    "Metadata access denied: document is private or access is not permitted"
            );

            throw new AccessDeniedException(
                    "You are not allowed to access this document");
        }

        return document;
    }

}
