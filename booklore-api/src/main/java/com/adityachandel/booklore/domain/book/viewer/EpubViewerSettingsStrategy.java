package com.adityachandel.booklore.domain.book.viewer;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.adityachandel.booklore.domain.book.viewer.BookViewerSettingsStrategy;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.enums.BookFileType;
import com.adityachandel.booklore.model.dto.BookViewerSettings;
import com.adityachandel.booklore.model.dto.EbookViewerPreferences;
import com.adityachandel.booklore.repository.EbookViewerPreferenceRepository;

@Service
@RequiredArgsConstructor
public class EpubViewerSettingsStrategy implements BookViewerSettingsStrategy {

    private final EbookViewerPreferenceRepository ebookViewerPreferenceRepository;

    @Override
    public boolean supports(BookFileType bookFileType) {
        return bookFileType == BookFileType.EPUB
                || bookFileType == BookFileType.FB2
                || bookFileType == BookFileType.MOBI
                || bookFileType == BookFileType.AZW3;
    }

    @Override
    public BookViewerSettings getSettings(BookEntity book, Long bookFileId, Long userId) {
        BookViewerSettings.BookViewerSettingsBuilder builder =
                BookViewerSettings.builder();

        ebookViewerPreferenceRepository
                .findByBookIdAndUserId(book.getId(), userId)
                .ifPresent(pref -> builder.ebookSettings(
                        EbookViewerPreferences.builder()
                                .bookId(book.getId())
                                .userId(userId)
                                .fontFamily(pref.getFontFamily())
                                .fontSize(pref.getFontSize())
                                .gap(pref.getGap())
                                .hyphenate(pref.getHyphenate())
                                .isDark(pref.getIsDark())
                                .justify(pref.getJustify())
                                .lineHeight(pref.getLineHeight())
                                .maxBlockSize(pref.getMaxBlockSize())
                                .maxColumnCount(pref.getMaxColumnCount())
                                .maxInlineSize(pref.getMaxInlineSize())
                                .theme(pref.getTheme())
                                .flow(pref.getFlow())
                                .build()
                ));

        return builder.build();
    }

    @Override
    public void updateSettings(BookEntity book, Long userId, BookViewerSettings settings) {
        
    }
}
