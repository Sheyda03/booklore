package com.adityachandel.booklore.domain.book.viewer;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.adityachandel.booklore.domain.book.viewer.BookViewerSettingsStrategy;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.enums.BookFileType;
import com.adityachandel.booklore.model.dto.BookViewerSettings;
import com.adityachandel.booklore.model.dto.CbxViewerPreferences;
import com.adityachandel.booklore.repository.CbxViewerPreferencesRepository;

@Service
@RequiredArgsConstructor
public class CbxViewerSettingsStrategy implements BookViewerSettingsStrategy {

    private final CbxViewerPreferencesRepository cbxViewerPreferencesRepository;

    @Override
    public boolean supports(BookFileType bookFileType) {
        return bookFileType == BookFileType.CBX;
    }

    @Override
    public BookViewerSettings getSettings(BookEntity book, Long bookFileId, Long userId) {
        BookViewerSettings.BookViewerSettingsBuilder builder =
                BookViewerSettings.builder();

        cbxViewerPreferencesRepository
                .findByBookIdAndUserId(book.getId(), userId)
                .ifPresent(pref -> builder.cbxSettings(
                        CbxViewerPreferences.builder()
                                .bookId(book.getId())
                                .pageViewMode(pref.getPageViewMode())
                                .pageSpread(pref.getPageSpread())
                                .fitMode(pref.getFitMode())
                                .scrollMode(pref.getScrollMode())
                                .backgroundColor(pref.getBackgroundColor())
                                .build()
                ));

        return builder.build();
    }

    @Override
    public void updateSettings(BookEntity book, Long userId, BookViewerSettings settings) {
        
    }
}

