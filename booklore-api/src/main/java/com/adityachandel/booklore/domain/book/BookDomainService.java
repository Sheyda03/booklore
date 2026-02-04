package com.adityachandel.booklore.domain.book;

import java.util.Set;

import com.adityachandel.booklore.model.dto.Shelf;

public interface BookDomainService {
    Set<Shelf> filterShelvesForUser(Set<Shelf> shelves, Long userId);
}

