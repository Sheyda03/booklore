package com.adityachandel.booklore.service.book;

import org.springframework.stereotype.Service;

import com.adityachandel.booklore.domain.book.BookDomainService;
import com.adityachandel.booklore.mapper.BookMapper;
import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.entity.UserBookFileProgressEntity;
import com.adityachandel.booklore.model.entity.UserBookProgressEntity;
import com.adityachandel.booklore.service.progress.ReadingProgressService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookAssembler {

    private final BookMapper bookMapper;
    private final BookDomainService bookDomainService;
    private final ReadingProgressService readingProgressService;

    public Book assemble(
            BookEntity bookEntity,
            Long userId,
            UserBookProgressEntity progress,
            UserBookFileProgressEntity fileProgress
    ) {
        Book book = bookMapper.toBook(bookEntity);
        book.setShelves(
                bookDomainService.filterShelvesForUser(book.getShelves(), userId)
        );
        readingProgressService.enrichBookWithProgress(
                book,
                progress,
                fileProgress
        );
        return book;
    }
}

