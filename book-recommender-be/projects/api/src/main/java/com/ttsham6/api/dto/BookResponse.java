package com.ttsham6.api.dto;

import com.ttsham6.shared.domain.Book;
import java.util.List;

public record BookResponse(int count, List<Book> items) {}
