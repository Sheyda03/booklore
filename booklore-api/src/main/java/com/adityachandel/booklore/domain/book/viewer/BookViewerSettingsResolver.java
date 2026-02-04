package com.adityachandel.booklore.domain.book.viewer;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.enums.BookFileType;
import com.adityachandel.booklore.model.dto.BookViewerSettings;
import com.adityachandel.booklore.exception.ApiError;

@Service
@RequiredArgsConstructor
public class BookViewerSettingsResolver {

    private final List<BookViewerSettingsStrategy> strategies;

    public BookViewerSettings resolve(
            BookEntity book,
            Long bookFileId,
            Long userId
    ) {
        BookFileType type = book.getPrimaryBookFile().getBookType();

        return strategies.stream()
                .filter(s -> s.supports(type))
                .findFirst()
                .orElseThrow(() -> ApiError.UNSUPPORTED_BOOK_TYPE.createException())
                .getSettings(book, bookFileId, userId);
    }

    public void update(
            BookEntity book,
            Long userId,
            BookViewerSettings settings
    ) {
        BookFileType type = book.getPrimaryBookFile().getBookType();

        strategies.stream()
                .filter(s -> s.supports(type))
                .findFirst()
                .orElseThrow(() -> ApiError.UNSUPPORTED_BOOK_TYPE.createException())
                .updateSettings(book, userId, settings);
    }
}

