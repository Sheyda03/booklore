package com.adityachandel.booklore.domain.book.viewer;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.enums.BookFileType;
import com.adityachandel.booklore.model.dto.BookViewerSettings;
import com.adityachandel.booklore.model.dto.PdfViewerPreferences;
import com.adityachandel.booklore.model.dto.NewPdfViewerPreferences;

import com.adityachandel.booklore.repository.PdfViewerPreferencesRepository;
import com.adityachandel.booklore.repository.NewPdfViewerPreferencesRepository;

@Service
@RequiredArgsConstructor
public class PdfViewerSettingsStrategy implements BookViewerSettingsStrategy {

    private final PdfViewerPreferencesRepository pdfRepo;
    private final NewPdfViewerPreferencesRepository newPdfRepo;

    @Override
    public boolean supports(BookFileType bookFileType) {
        return bookFileType == BookFileType.PDF;
    }

    @Override
    public BookViewerSettings getSettings(BookEntity book, Long bookFileId, Long userId) {
        BookViewerSettings.BookViewerSettingsBuilder builder =
                BookViewerSettings.builder();

        pdfRepo.findByBookIdAndUserId(book.getId(), userId)
                .ifPresent(p -> builder.pdfSettings(
                        PdfViewerPreferences.builder()
                                .bookId(book.getId())
                                .zoom(p.getZoom())
                                .spread(p.getSpread())
                                .build()
                ));

        newPdfRepo.findByBookIdAndUserId(book.getId(), userId)
                .ifPresent(p -> builder.newPdfSettings(
                        NewPdfViewerPreferences.builder()
                                .bookId(book.getId())
                                .pageViewMode(p.getPageViewMode())
                                .pageSpread(p.getPageSpread())
                                .fitMode(p.getFitMode())
                                .scrollMode(p.getScrollMode())
                                .backgroundColor(p.getBackgroundColor())
                                .build()
                ));

        return builder.build();
    }

    @Override
    public void updateSettings(BookEntity book, Long userId, BookViewerSettings settings) {
        
    }
}

