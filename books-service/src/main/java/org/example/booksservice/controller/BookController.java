package org.example.booksservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.booksservice.dto.request.BookDto;
import org.example.booksservice.dto.response.BookResponse;
import org.example.booksservice.dto.response.BookResponseWithReview;
import org.example.booksservice.entity.Book;
import org.example.booksservice.service.BookService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping(value = "/create", consumes = "multipart/form-data")
    public Book createBook(@ModelAttribute BookDto bookDto) {
        return bookService.createBook(bookDto);
    }



}
