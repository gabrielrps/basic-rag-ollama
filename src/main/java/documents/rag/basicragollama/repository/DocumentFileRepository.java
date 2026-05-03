package documents.rag.basicragollama.repository;

import documents.rag.basicragollama.entity.DocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentFileRepository extends JpaRepository<DocumentFile, Long> {

    Optional<DocumentFile> findByFileHash(String fileHash);
    Optional<DocumentFile> findByFileName(String fileName);

}

