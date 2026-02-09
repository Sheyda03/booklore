package com.adityachandel.booklore.service.book;

import com.adityachandel.booklore.domain.book.viewer.BookViewerSettingsResolver;
import com.adityachandel.booklore.domain.book.BookDomainService;
import com.adityachandel.booklore.config.security.service.AuthenticationService;
import com.adityachandel.booklore.exception.ApiError;
import com.adityachandel.booklore.mapper.BookMapper;
import com.adityachandel.booklore.model.dto.*;
import com.adityachandel.booklore.model.dto.request.ReadProgressRequest;
import com.adityachandel.booklore.model.dto.response.BookDeletionResponse;
import com.adityachandel.booklore.model.dto.response.BookStatusUpdateResponse;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.entity.BookFileEntity;
import com.adityachandel.booklore.model.entity.UserBookFileProgressEntity;
import com.adityachandel.booklore.model.entity.UserBookProgressEntity;
import com.adityachandel.booklore.model.enums.BookFileType;
import com.adityachandel.booklore.repository.*;
import com.adityachandel.booklore.service.progress.ReadingProgressService;
import com.adityachandel.booklore.util.FileService;
import com.adityachandel.booklore.util.FileUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class BookService {
    private final BookViewerSettingsResolver bookViewerSettingsResolver;
    
    private final BookRepository bookRepository;
    private final FileService fileService;
    private final UserBookProgressRepository userBookProgressRepository;
    private final AuthenticationService authenticationService;
    private final BookQueryService bookQueryService;
    private final ReadingProgressService readingProgressService;
    private final BookDownloadService bookDownloadService;
    private final BookUpdateService bookUpdateService;
    private final BookFileCleanupService bookFileCleanupService;
    private final BookAssembler bookAssembler;

    public List<Book> getBookDTOs(boolean includeDescription) {
        BookLoreUser user = authenticationService.getAuthenticatedUser();
        boolean isAdmin = user.getPermissions().isAdmin();

        List<BookEntity> bookEntities = isAdmin
                ? bookQueryService.getAllBookEntities(includeDescription)
                : bookQueryService.getAllBookEntitiesByLibraryIds(
                        user.getAssignedLibraries().stream()
                                .map(Library::getId)
                                .collect(Collectors.toSet())
                );

        Set<Long> bookIds = bookEntities.stream()
                .map(BookEntity::getId)
                .collect(Collectors.toSet());

        Map<Long, UserBookProgressEntity> progressMap =
                readingProgressService.fetchUserProgress(user.getId(), bookIds);

        Map<Long, UserBookFileProgressEntity> fileProgressMap =
                readingProgressService.fetchUserFileProgress(user.getId(), bookIds);

        return bookEntities.stream()
                .map(entity -> bookAssembler.assemble(
                        entity,
                        user.getId(),
                        progressMap.get(entity.getId()),
                        fileProgressMap.get(entity.getId())
                ))
                .peek(book -> {
                    if (!includeDescription) {
                        book.getMetadata().setDescription(null);
                    }
                })
                .collect(Collectors.toList());
    }


    public List<Book> getBooksByIds(Set<Long> bookIds, boolean withDescription) {
        BookLoreUser user = authenticationService.getAuthenticatedUser();

        List<BookEntity> bookEntities =
                bookQueryService.findAllWithMetadataByIds(bookIds);

        Set<Long> entityIds = bookEntities.stream()
                .map(BookEntity::getId)
                .collect(Collectors.toSet());

        Map<Long, UserBookProgressEntity> progressMap =
                readingProgressService.fetchUserProgress(user.getId(), entityIds);

        Map<Long, UserBookFileProgressEntity> fileProgressMap =
                readingProgressService.fetchUserFileProgress(user.getId(), entityIds);

        return bookEntities.stream()
                .map(entity -> bookAssembler.assemble(
                        entity,
                        user.getId(),
                        progressMap.get(entity.getId()),
                        fileProgressMap.get(entity.getId())
                ))
                .peek(book -> {
                    if (!withDescription) {
                        book.getMetadata().setDescription(null);
                    }
                })
                .collect(Collectors.toList());
    }

    public Book getBook(long bookId, boolean withDescription) {
        BookLoreUser user = authenticationService.getAuthenticatedUser();

        BookEntity bookEntity = bookRepository.findByIdWithBookFiles(bookId)
                .orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));

        UserBookProgressEntity userProgress =
                userBookProgressRepository.findByUserIdAndBookId(user.getId(), bookId)
                        .orElse(new UserBookProgressEntity());

        // Fetch file-level progress for the book (most recent across all files)
        UserBookFileProgressEntity fileProgress = readingProgressService
                .fetchUserFileProgress(user.getId(), Set.of(bookId))
                .get(bookId);

        Book book = bookAssembler.assemble(
                bookEntity,
                user.getId(),
                userProgress,
                fileProgress
        );

        if (!withDescription) {
            book.getMetadata().setDescription(null);
        }

        return book;
    }

    
    public BookViewerSettings getBookViewerSetting(long bookId, long bookFileId) {
        BookEntity bookEntity = bookRepository.findByIdWithBookFiles(bookId)
                .orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));

        BookLoreUser user = authenticationService.getAuthenticatedUser();

        return bookViewerSettingsResolver.resolve(bookEntity, bookFileId, user.getId());
    }    

    public void updateBookViewerSetting(long bookId, BookViewerSettings bookViewerSettings) {
        bookUpdateService.updateBookViewerSetting(bookId, bookViewerSettings);
    }

    @Transactional
    public void updateReadProgress(ReadProgressRequest request) {
        readingProgressService.updateReadProgress(request);
    }

    @Transactional
    public List<BookStatusUpdateResponse> updateReadStatus(List<Long> bookIds, String status) {
        return bookUpdateService.updateReadStatus(bookIds, status);
    }

    @Transactional
    public List<Book> assignShelvesToBooks(Set<Long> bookIds, Set<Long> shelfIdsToAssign, Set<Long> shelfIdsToUnassign) {
        return bookUpdateService.assignShelvesToBooks(bookIds, shelfIdsToAssign, shelfIdsToUnassign);
    }

    public Resource getBookThumbnail(long bookId) {
        Path thumbnailPath = Paths.get(fileService.getThumbnailFile(bookId));
        try {
            if (Files.exists(thumbnailPath)) {
                return new UrlResource(thumbnailPath.toUri());
            } else {
                Path defaultCover = Paths.get("static/images/missing-cover.jpg");
                return new UrlResource(defaultCover.toUri());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to load book cover for bookId=" + bookId, e);
        }
    }

    public Resource getBookCover(long bookId) {
        Path coverPath = Paths.get(fileService.getCoverFile(bookId));
        try {
            if (Files.exists(coverPath)) {
                return new UrlResource(coverPath.toUri());
            } else {
                return new ClassPathResource("static/images/missing-cover.jpg");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to load book cover for bookId=" + bookId, e);
        }
    }

    public Resource getBookCover(String coverHash) {
        BookEntity bookEntity = bookRepository.findByBookCoverHash(coverHash).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(coverHash));
        return getBookCover(bookEntity.getId());
    }

    public ResponseEntity<Resource> downloadBook(Long bookId) {
        return bookDownloadService.downloadBook(bookId);
    }

    public void downloadAllBookFiles(Long bookId, HttpServletResponse response) {
        bookDownloadService.downloadAllBookFiles(bookId, response);
    }

    public ResponseEntity<ByteArrayResource> getBookContent(long bookId) throws IOException {
        return getBookContent(bookId, null);
    }

    public ResponseEntity<ByteArrayResource> getBookContent(long bookId, String bookType) throws IOException {
        BookEntity bookEntity = bookRepository.findById(bookId).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));
        String filePath;
        if (bookType != null) {
            BookFileType requestedType = BookFileType.valueOf(bookType.toUpperCase());
            BookFileEntity bookFile = bookEntity.getBookFiles().stream()
                    .filter(bf -> bf.getBookType() == requestedType)
                    .findFirst()
                    .orElseThrow(() -> ApiError.FILE_NOT_FOUND.createException("No file of type " + bookType + " found for book"));
            filePath = bookFile.getFullFilePath().toString();
        } else {
            filePath = FileUtils.getBookFullPath(bookEntity);
        }
        try (FileInputStream inputStream = new FileInputStream(filePath)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new ByteArrayResource(inputStream.readAllBytes()));
        }
    }

    @Transactional
    public ResponseEntity<BookDeletionResponse> deleteBooks(Set<Long> ids) {

        List<BookEntity> books = bookQueryService.findAllWithMetadataByIds(ids);
        List<Long> failedFileDeletions = new ArrayList<>();

        for (BookEntity book : books) {
            
            bookFileCleanupService.cleanupAfterBookDeletion(book, failedFileDeletions);
        }

        bookRepository.deleteAll(books);

        BookDeletionResponse response =
                new BookDeletionResponse(ids, failedFileDeletions);

        return failedFileDeletions.isEmpty()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.MULTI_STATUS).body(response);
    }
}

