package com.adityachandel.booklore.domain.book;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.adityachandel.booklore.model.dto.Shelf;

@Service
public class BookDomainServiceImpl implements BookDomainService {

    @Override
    public Set<Shelf> filterShelvesForUser(Set<Shelf> shelves, Long userId) {
        if (shelves == null) return Collections.emptySet();
        return shelves.stream()
                .filter(shelf -> userId.equals(shelf.getUserId()))
                .collect(Collectors.toSet());
    }
}
