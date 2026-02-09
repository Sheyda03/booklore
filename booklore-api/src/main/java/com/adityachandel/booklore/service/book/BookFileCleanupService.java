package com.adityachandel.booklore.service.book;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.entity.BookFileEntity;
import com.adityachandel.booklore.model.entity.LibraryPathEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class BookFileCleanupService {
    
    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;

        try (var walk = Files.walk(path)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
        }
    }

    public void deleteEmptyParentDirsUpToLibraryFolders(Path currentDir, Set<Path> libraryRoots) {
        Path dir = currentDir;
        Set<String> ignoredFilenames = Set.of(".DS_Store", "Thumbs.db");
        dir = dir.toAbsolutePath().normalize();

        Set<Path> normalizedRoots = new HashSet<>();
        for (Path root : libraryRoots) {
            normalizedRoots.add(root.toAbsolutePath().normalize());
        }

        while (dir != null) {
            boolean isLibraryRoot = false;
            for (Path root : normalizedRoots) {
                try {
                    if (Files.isSameFile(root, dir)) {
                        isLibraryRoot = true;
                        break;
                    }
                } catch (IOException e) {
                    log.warn("Failed to compare paths: {} and {}", root, dir);
                }
            }

            if (isLibraryRoot) {
                log.debug("Reached library root: {}. Stopping cleanup.", dir);
                break;
            }

            File[] files = dir.toFile().listFiles();
            if (files == null) {
                log.warn("Cannot read directory: {}. Stopping cleanup.", dir);
                break;
            }

            boolean hasImportantFiles = false;
            for (File file : files) {
                if (!ignoredFilenames.contains(file.getName())) {
                    hasImportantFiles = true;
                    break;
                }
            }

            if (!hasImportantFiles) {
                for (File file : files) {
                    try {
                        Files.delete(file.toPath());
                        log.info("Deleted ignored file: {}", file.getAbsolutePath());
                    } catch (IOException e) {
                        log.warn("Failed to delete ignored file: {}", file.getAbsolutePath());
                    }
                }
                try {
                    Files.delete(dir);
                    log.info("Deleted empty directory: {}", dir);
                } catch (IOException e) {
                    log.warn("Failed to delete directory: {}", dir, e);
                    break;
                }
                dir = dir.getParent();
            } else {                
                log.debug("Directory {} contains important files. Stopping cleanup.", dir);
                break;
            }
        }
    }
    public void cleanupAfterBookDeletion(BookEntity book, List<Long> failedFileDeletions) {
    for (BookFileEntity bookFile : book.getBookFiles()) {
        Path fullFilePath = bookFile.getFullFilePath();
        try {
            if (Files.exists(fullFilePath)) {

                // Folder-based audiobooks
                if (bookFile.isFolderBased() && Files.isDirectory(fullFilePath)) {
                    deleteDirectoryRecursively(fullFilePath);
                    log.info("Deleted folder-based audiobook: {}", fullFilePath);
                } else {
                    Files.delete(fullFilePath);
                    log.info("Deleted book file: {}", fullFilePath);
                }

                Set<Path> libraryRoots = book.getLibrary().getLibraryPaths().stream()
                        .map(LibraryPathEntity::getPath)
                        .map(Paths::get)
                        .map(Path::normalize)
                        .collect(Collectors.toSet());

                deleteEmptyParentDirsUpToLibraryFolders(fullFilePath.getParent(), libraryRoots);
            }
        } catch (IOException e) {
            log.warn("Failed to delete book file: {}", fullFilePath, e);
            failedFileDeletions.add(book.getId());
        }
    }
}

}
