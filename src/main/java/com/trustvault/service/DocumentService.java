
package com.trustvault.service;

import com.trustvault.entity.Document;
import com.trustvault.entity.Role;
import com.trustvault.entity.User;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.net.MalformedURLException;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final Path storageDirectory;

    public DocumentService(
            DocumentRepository documentRepository,
            UserRepository userRepository,
            @Value("${trustvault.storage-dir:./trustvault-files}")
            String storageDirectory) {

        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.storageDirectory = Paths.get(storageDirectory)
                .toAbsolutePath()
                .normalize();
    }

    @Transactional
    public Document uploadDocument(
            String username,
            MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a non-empty file");
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

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (owner.getRole() != com.trustvault.entity.Role.DOCUMENT_OWNER) {
            throw new AccessDeniedException(
                    "Only Document Owners can upload files");
        }

        Files.createDirectories(storageDirectory);

        String originalName = Paths.get(file.getOriginalFilename() == null
                        ? "document"
                        : file.getOriginalFilename())
                .getFileName()
                .toString();

        String storedName = UUID.randomUUID() + ".bin";
        Path destination = storageDirectory.resolve(storedName).normalize();

        if (!destination.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        Files.copy(file.getInputStream(), destination,
                StandardCopyOption.REPLACE_EXISTING);

        try {
            Document document = new Document();
            document.setFileName(originalName);
            document.setContentType(contentType);
            document.setStoragePath(destination.toString());
            document.setOwnerId(owner.getId());

            return documentRepository.save(document);
        } catch (RuntimeException ex) {
            Files.deleteIfExists(destination);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<Document> getMyDocuments(String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (owner.getRole() != com.trustvault.entity.Role.DOCUMENT_OWNER) {
            throw new AccessDeniedException(
                    "Only Document Owners can list their documents");
        }

        return documentRepository.findByOwnerId(owner.getId());
    }

    @Transactional(readOnly = true)
    public Resource downloadMyDocument(String username, Long documentId)
            throws MalformedURLException {

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (owner.getRole() != Role.DOCUMENT_OWNER) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only Document Owners can download documents");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Document not found"));

        if (!document.getOwnerId().equals(owner.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot download another owner's document");
        }

        Path filePath = Paths.get(document.getStoragePath())
                .toAbsolutePath()
                .normalize();

        if (!filePath.startsWith(storageDirectory)
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

        return resource;
    }
}
