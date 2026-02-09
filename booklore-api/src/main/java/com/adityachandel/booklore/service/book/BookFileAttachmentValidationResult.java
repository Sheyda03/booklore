package com.adityachandel.booklore.service.book;

import java.util.List;

import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.entity.BookFileEntity;

public record BookFileAttachmentValidationResult(
        BookEntity targetBook,
        List<BookEntity> sourceBooks,
        BookFileEntity targetPrimaryFile
) {}

