package org.example.booksservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.booksservice.dto.response.BookResponse;
import org.example.booksservice.dto.response.BookResponseWithReview;
import org.example.booksservice.dto.response.ReviewRequest;
import org.example.booksservice.entity.Book;
import org.example.booksservice.repository.BookRepository;
import org.example.booksservice.service.BookService;
import org.example.booksservice.service.DownloadService;
import org.example.booksservice.service.IdService;
import org.example.booksservice.service.SearchBookService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchBookServiceImpl implements SearchBookService {

    private final BookRepository bookRepository;
    private final IdService idService;
    private final DownloadService downloadService;
    private final BookService bookService;

    public BookResponseWithReview getBookWithTitle(String title) {
        Book book = bookRepository.findByTitle(title)
                .orElseThrow(() -> new IllegalArgumentException("Книга с таким названием не найдена"));

        downloadService.saveDownload(book.getId(), idService.getUserId());

        BookResponse bookResponse = bookService.mapBookToResponse(book);
        ReviewRequest review = idService.getReview(book.getId());

        return BookResponseWithReview.builder()
                .bookResponse(bookResponse)
                .review(review)
                .build();
    }

    public BookResponse getBookWithAuthor(String author) {
        Book book = bookRepository.findAllByAuthor(author)
                .orElseThrow(() -> new IllegalArgumentException("Книга с таким автором не найдена"));

        return bookService.mapBookToResponse(book);
    }

    public BookResponse getBookWithGenre(String genre) {
        Book book = bookRepository.findAllByGenre(genre)
                .orElseThrow(() -> new IllegalArgumentException("Книга с таким жанром не найдена"));

        return bookService.mapBookToResponse(book);
    }

}