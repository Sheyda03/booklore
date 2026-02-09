package com.adityachandel.booklore.domain.book.viewer;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.adityachandel.booklore.domain.book.viewer.BookViewerSettingsStrategy;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.entity.CbxViewerPreferencesEntity;
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
        if (settings.getCbxSettings() == null) return;

        long bookId = book.getId();

        CbxViewerPreferencesEntity prefs = cbxViewerPreferencesRepository
                .findByBookIdAndUserId(bookId, userId)
                .orElseGet(() -> cbxViewerPreferencesRepository.save(
                        CbxViewerPreferencesEntity.builder()
                                .bookId(bookId)
                                .userId(userId)
                                .build()
                ));

        CbxViewerPreferences cbxSettings = settings.getCbxSettings();

        prefs.setPageSpread(cbxSettings.getPageSpread());
        prefs.setPageViewMode(cbxSettings.getPageViewMode());
        prefs.setFitMode(cbxSettings.getFitMode());
        prefs.setScrollMode(cbxSettings.getScrollMode());
        prefs.setBackgroundColor(cbxSettings.getBackgroundColor());

        cbxViewerPreferencesRepository.save(prefs);
    }
}

