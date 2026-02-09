package com.adityachandel.booklore.service.book;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.adityachandel.booklore.exception.ApiError;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.entity.BookFileEntity;
import com.adityachandel.booklore.repository.BookRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookFileAttachmentValidator {

    private final BookRepository bookRepository;

    public BookFileAttachmentValidationResult validate(
            Long targetBookId,
            List<Long> sourceBookIds
    ) {

        BookEntity targetBook = bookRepository.findByIdWithBookFiles(targetBookId)
                .orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(targetBookId));

        Set<Long> uniqueSourceBookIds = new LinkedHashSet<>(sourceBookIds);
        if (uniqueSourceBookIds.contains(targetBookId)) {
            throw ApiError.GENERIC_BAD_REQUEST.createException("Cannot attach a book to itself");
        }

        List<BookEntity> sourceBooks = new ArrayList<>();
        for (Long sourceBookId : uniqueSourceBookIds) {
            BookEntity sourceBook = bookRepository.findByIdWithBookFiles(sourceBookId)
                    .orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(sourceBookId));
            sourceBooks.add(sourceBook);
        }

        BookFileEntity targetPrimaryFile = targetBook.getBookFiles().stream()
                .filter(BookFileEntity::isBookFormat)
                .findFirst()
                .orElseThrow(() ->
                        ApiError.GENERIC_BAD_REQUEST.createException("Target book has no primary file")
                );

        for (BookEntity sourceBook : sourceBooks) {

            if (!targetBook.getLibrary().getId().equals(sourceBook.getLibrary().getId())) {
                throw ApiError.GENERIC_BAD_REQUEST.createException(
                        "Source book " + sourceBook.getId() + " must be in the same library as target"
                );
            }

            List<BookFileEntity> sourceBookFiles = sourceBook.getBookFiles().stream()
                    .filter(BookFileEntity::isBookFormat)
                    .toList();

            if (sourceBookFiles.isEmpty()) {
                throw ApiError.GENERIC_BAD_REQUEST.createException(
                        "Source book " + sourceBook.getId() + " has no book format files to attach"
                );
            }

            if (sourceBookFiles.size() > 1) {
                throw ApiError.GENERIC_BAD_REQUEST.createException(
                        "Source book " + sourceBook.getId() + " has multiple book format files"
                );
            }

            BookFileEntity fileToMove = sourceBookFiles.get(0);

            if (fileToMove.isFolderBased()) {
                throw ApiError.GENERIC_BAD_REQUEST.createException(
                        "Source book " + sourceBook.getId() + " is a folder-based audiobook"
                );
            }

            if (!Files.exists(fileToMove.getFullFilePath())) {
                throw ApiError.GENERIC_BAD_REQUEST.createException(
                        "Source file not found: " + fileToMove.getFullFilePath()
                );
            }
        }

        return new BookFileAttachmentValidationResult(
                targetBook,
                sourceBooks,
                targetPrimaryFile
        );
    }
}


