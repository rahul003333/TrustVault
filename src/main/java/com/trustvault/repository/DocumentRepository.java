
package com.trustvault.repository;

import com.trustvault.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByOwnerId(Long ownerId);

    List<Document> findByPubliclySharedTrue();

    boolean existsByIdAndOwnerId(Long id, Long ownerId);
    List<Document> findByOwnerIdOrPubliclySharedTrue(Long ownerId);
}
