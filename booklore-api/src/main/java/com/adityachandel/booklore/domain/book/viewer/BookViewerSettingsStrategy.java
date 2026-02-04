package com.adityachandel.booklore.domain.book.viewer;

import com.adityachandel.booklore.model.dto.BookViewerSettings;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.enums.BookFileType;

public interface BookViewerSettingsStrategy {

    boolean supports(BookFileType bookFileType);

    BookViewerSettings getSettings(
            BookEntity book,
            Long bookFileId,
            Long userId
    );

    void updateSettings(
            BookEntity book,
            Long userId,
            BookViewerSettings settings
    );
}
